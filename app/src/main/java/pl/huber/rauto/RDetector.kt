package pl.huber.rauto

import kotlin.math.max
import kotlin.math.sqrt
import java.util.ArrayDeque

/**
 * Experimental, fully-offline detector for a Polish alveolar/trilled "r".
 *
 * It is intentionally a game signal, not a diagnostic tool. The detector looks for:
 *  - voiced/periodic speech,
 *  - repeated short envelope dips/rebounds that may correspond to tongue contacts,
 *  - enough signal energy while rejecting click-like/noisy frames.
 *
 * No audio is persisted or sent anywhere.
 */
class RDetector(private val sampleRate: Int = 16_000) {
    data class Evaluation(
        val score: Int,
        val detected: Boolean,
        val db: Double,
        val voicing: Double,
        val trill: Double,
        val noisePenalty: Double
    )

    private val window = ArrayDeque<Short>()
    private val maxSamples = sampleRate * 600 / 1000 // 600 ms
    private var latched = false
    private var quietMs = 0
    private var strongMs = 0

    fun reset() {
        window.clear(); latched = false; quietMs = 0; strongMs = 0
    }

    fun accept(samples: ShortArray, count: Int, db: Double, durationMs: Int): Evaluation {
        if (count <= 0) return Evaluation(0, false, db, 0.0, 0.0, 1.0)
        for (i in 0 until count) {
            window.addLast(samples[i])
            while (window.size > maxSamples) window.removeFirst()
        }

        if (db < -48.0) {
            quietMs += durationMs
            strongMs = 0
            if (quietMs >= 280) latched = false
            return Evaluation(0, false, db, 0.0, 0.0, 0.0)
        }
        quietMs = 0

        if (window.size < sampleRate / 4) {
            return Evaluation(0, false, db, 0.0, 0.0, 0.0)
        }

        val data = DoubleArray(window.size)
        var idx = 0
        for (s in window) data[idx++] = s / 32768.0

        val voicing = estimateVoicing(data)
        val trill = estimateTrill(data)
        val zcr = zeroCrossingRate(data)
        // High ZCR is typical for hiss/click/noise. Normal voiced speech should score little penalty.
        val noisePenalty = ((zcr - 0.18) / 0.22).coerceIn(0.0, 1.0)
        val energy = ((db + 46.0) / 24.0).coerceIn(0.0, 1.0)

        var raw = 100.0 * (
            0.38 * voicing +
            0.47 * trill +
            0.15 * energy -
            0.28 * noisePenalty
        )
        // A trill without voicing is much more likely to be environmental noise.
        if (voicing < 0.28) raw *= 0.55
        // Flat voiced vowels can be loud and periodic, but should not pass without envelope modulation.
        if (trill < 0.22) raw *= 0.65
        val score = raw.toInt().coerceIn(0, 100)

        if (score >= 67) strongMs += durationMs else strongMs = max(0, strongMs - durationMs * 2)
        val detected = !latched && strongMs >= 120
        if (detected) latched = true
        return Evaluation(score, detected, db, voicing, trill, noisePenalty)
    }

    private fun estimateVoicing(x: DoubleArray): Double {
        // Normalized autocorrelation. Lags 40..200 samples ~= 80..400 Hz at 16 kHz.
        val start = max(0, x.size - sampleRate * 300 / 1000)
        var mean = 0.0
        for (i in start until x.size) mean += x[i]
        mean /= (x.size - start)
        var energy = 0.0
        for (i in start until x.size) {
            val v = x[i] - mean
            energy += v * v
        }
        if (energy < 1e-8) return 0.0
        var best = 0.0
        val minLag = (sampleRate / 400).coerceAtLeast(1)
        val maxLag = (sampleRate / 80).coerceAtMost((x.size - start) / 2)
        for (lag in minLag..maxLag step 2) {
            var corr = 0.0
            var e1 = 0.0
            var e2 = 0.0
            var i = start + lag
            while (i < x.size) {
                val a = x[i] - mean
                val b = x[i - lag] - mean
                corr += a * b; e1 += a * a; e2 += b * b
                i += 2 // enough for a robust game score and cheaper on mobile
            }
            val norm = corr / sqrt((e1 * e2).coerceAtLeast(1e-12))
            if (norm > best) best = norm
        }
        return ((best - 0.18) / 0.62).coerceIn(0.0, 1.0)
    }

    private fun estimateTrill(x: DoubleArray): Double {
        // RMS envelope in 10 ms blocks over the most recent ~500 ms.
        val block = sampleRate / 100
        val wanted = sampleRate / 2
        val start = max(0, x.size - wanted)
        val env = ArrayList<Double>()
        var p = start
        while (p + block <= x.size) {
            var e = 0.0
            for (i in p until p + block) e += x[i] * x[i]
            env.add(sqrt(e / block))
            p += block
        }
        if (env.size < 20) return 0.0
        val mean = env.average().coerceAtLeast(1e-6)
        val normalized = env.map { it / mean }

        var contacts = 0
        var reboundStrength = 0.0
        // Look for short local valleys followed by a rebound. Avoid requiring a textbook long trill.
        for (i in 1 until normalized.lastIndex) {
            val prev = normalized[i - 1]
            val cur = normalized[i]
            val next = normalized[i + 1]
            if (cur < prev * 0.78 && cur < next * 0.78 && cur < 0.82) {
                contacts++
                reboundStrength += ((prev + next) * 0.5 - cur).coerceAtLeast(0.0)
            }
        }

        // Envelope coefficient of variation helps separate a flat vowel from repeated contacts.
        var variance = 0.0
        for (v in normalized) variance += (v - 1.0) * (v - 1.0)
        val cv = sqrt(variance / normalized.size)
        val contactScore = when (contacts) {
            0 -> 0.0
            1 -> 0.30
            2 -> 0.62
            3 -> 0.82
            else -> 1.0
        }
        val reboundScore = (reboundStrength / 1.2).coerceIn(0.0, 1.0)
        val variationScore = ((cv - 0.10) / 0.42).coerceIn(0.0, 1.0)
        return (0.55 * contactScore + 0.25 * reboundScore + 0.20 * variationScore).coerceIn(0.0, 1.0)
    }

    private fun zeroCrossingRate(x: DoubleArray): Double {
        val start = max(1, x.size - sampleRate * 200 / 1000)
        var crossings = 0
        for (i in start until x.size) {
            if ((x[i] >= 0) != (x[i - 1] >= 0)) crossings++
        }
        return crossings.toDouble() / (x.size - start).coerceAtLeast(1)
    }
}
