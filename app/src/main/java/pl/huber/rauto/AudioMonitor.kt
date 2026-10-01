package pl.huber.rauto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.sqrt

/** Ephemeral PCM, no file output, no networking. Each capture owns its recorder. */
class AudioMonitor(private val context: Context) {
    private class Capture(val recorder: AudioRecord) { @Volatile var active = true }
    private var capture: Capture? = null

    @Synchronized
    fun start(onFrame: (Double, Int, ShortArray, Int) -> Unit, onError: (String) -> Unit) {
        stop()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Brak zgody na dostęp do mikrofonu.")
            return
        }
        var recorder: AudioRecord? = null
        try {
            val rate = 16000
            val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0) { "Telefon nie obsługuje wybranego formatu mikrofonu." }
            val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 6400))
            recorder = audio
            check(audio.state == AudioRecord.STATE_INITIALIZED) { "Mikrofon jest niedostępny." }
            audio.startRecording()
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Nie można rozpocząć nasłuchu." }
            val current = Capture(audio)
            capture = current
            Thread({
                try {
                    val samples = ShortArray(800) // 50 ms @ 16 kHz
                    while (current.active) {
                        val count = audio.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        if (!current.active) break
                        check(count > 0) { "Przerwano odczyt mikrofonu. Spróbuj ponownie." }
                        var energy = 0.0
                        for (i in 0 until count) { val v = samples[i] / 32768.0; energy += v * v }
                        val rms = sqrt(energy / count)
                        val db = (20 * log10(rms.coerceAtLeast(0.000001))).coerceIn(-120.0, 0.0)
                        // Copy because AudioRecord reuses the buffer immediately on the next read.
                        onFrame(db, (count * 1000 / rate).coerceAtLeast(1), samples.copyOf(count), count)
                    }
                } catch (e: Exception) {
                    if (current.active) onError(e.message ?: "Błąd mikrofonu")
                } finally {
                    current.active = false
                    try { audio.stop() } catch (_: Exception) { }
                    audio.release()
                }
            }, "RAuto-Microphone").start()
        } catch (e: Exception) {
            recorder?.release()
            onError(e.message ?: "Nie można uruchomić mikrofonu.")
        }
    }

    @Synchronized
    fun stop() {
        capture?.let {
            it.active = false
            try { it.recorder.stop() } catch (_: Exception) { }
        }
        capture = null
    }
}
