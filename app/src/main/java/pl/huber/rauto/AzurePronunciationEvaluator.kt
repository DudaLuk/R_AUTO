package pl.huber.rauto

import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import java.util.concurrent.Executors

/** Azure Speech Pronunciation Assessment. Audio is sent only when the child starts an attempt. */
class AzurePronunciationEvaluator {
    data class Result(val score: Double, val recognizedText: String, val error: String? = null)
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var recognizer: SpeechRecognizer? = null

    fun assess(referenceText: String, key: String, region: String, callback: (Result) -> Unit) {
        if (key.isBlank() || region.isBlank()) {
            callback(Result(0.0, "", "Uzupełnij klucz i region Azure Speech.")); return
        }
        executor.execute {
            var speech: SpeechConfig? = null
            var audio: AudioConfig? = null
            try {
                speech = SpeechConfig.fromSubscription(key.trim(), region.trim())
                speech!!.speechRecognitionLanguage = "pl-PL"
                audio = AudioConfig.fromDefaultMicrophoneInput()
                val r = SpeechRecognizer(speech, audio)
                recognizer = r
                val assessment = PronunciationAssessmentConfig(
                    referenceText.trim(),
                    PronunciationAssessmentGradingSystem.HundredMark,
                    PronunciationAssessmentGranularity.Phoneme,
                    true
                )
                assessment.applyTo(r)
                val result = r.recognizeOnceAsync().get()
                if (result.reason == ResultReason.RecognizedSpeech) {
                    val pa = PronunciationAssessmentResult.fromResult(result)
                    callback(Result(pa.accuracyScore, result.text))
                } else {
                    callback(Result(0.0, result.text ?: "", "Nie udało się rozpoznać wypowiedzi. Spróbuj ponownie."))
                }
                r.close()
            } catch (e: Exception) {
                callback(Result(0.0, "", e.message ?: "Błąd oceny wymowy."))
            } finally {
                recognizer = null
                audio?.close(); speech?.close()
            }
        }
    }
    fun cancel() { try { recognizer?.stopContinuousRecognitionAsync()?.get() } catch (_: Exception) { } }
    fun close() { cancel(); executor.shutdownNow() }
}
