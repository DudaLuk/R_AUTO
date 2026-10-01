package pl.huber.rauto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class RDetectorTest {
    private val sampleRate = 16_000

    @Test fun flatVoicedToneDoesNotTriggerButTrillLikeModulationDoes() {
        assertFalse(runSignal(modulated = false))
        assertTrue(runSignal(modulated = true))
    }

    private fun runSignal(modulated: Boolean): Boolean {
        val detector = RDetector(sampleRate)
        var detected = false
        repeat(16) { frameIndex ->
            val frame = ShortArray(800) { i ->
                val n = frameIndex * 800 + i
                val envelope = if (modulated) {
                    0.55 + 0.45 * (0.5 + 0.5 * sin(2.0 * PI * 24.0 * n / sampleRate))
                } else 1.0
                (sin(2.0 * PI * 140.0 * n / sampleRate) * 0.35 * envelope * 32767.0)
                    .toInt().coerceIn(-32768, 32767).toShort()
            }
            val result = detector.accept(frame, frame.size, db(frame), 50)
            detected = detected || result.detected
        }
        return detected
    }

    private fun db(samples: ShortArray): Double {
        var energy = 0.0
        for (sample in samples) {
            val value = sample / 32768.0
            energy += value * value
        }
        return 20 * log10(sqrt(energy / samples.size).coerceAtLeast(0.000001))
    }
}
