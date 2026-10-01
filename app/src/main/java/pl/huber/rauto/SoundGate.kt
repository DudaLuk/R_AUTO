package pl.huber.rauto

/** Energy gate only. Does NOT identify speech, phonemes or pronunciation correctness. */
class SoundGate {
    private var audibleMs = 0
    private var quietMs = 0
    private var latched = false
    fun accept(db: Double, threshold: Double, durationMs: Int): Boolean {
        if (db >= threshold) {
            quietMs = 0
            audibleMs += durationMs
            if (!latched && audibleMs >= 200) { latched = true; return true }
        } else {
            audibleMs = 0
            quietMs += durationMs
            if (quietMs >= 400) latched = false
        }
        return false
    }
}
