package pl.huber.rauto

/** Independent from Android; time supplied by the monotonic UI clock. */
class RaceEngine {
    var running = false
        private set
    var remaining = 60f
        private set
    var speed = 0f
        private set
    var distance = 0f
        private set
    var stars = 0
        private set
    private var boost = 0f

    private var baseSpeed = 18f
    private var boostSpeed = 85f
    private var boostDuration = 3.5f
    private var acceleration = 2f

    fun configure(baseSpeed: Float, boostSpeed: Float, boostDuration: Float, acceleration: Float) {
        this.baseSpeed = baseSpeed.coerceIn(10f, 60f)
        this.boostSpeed = boostSpeed.coerceIn(this.baseSpeed + 10f, 160f)
        this.boostDuration = boostDuration.coerceIn(1f, 10f)
        this.acceleration = acceleration.coerceIn(0.5f, 8f)
    }

    fun start(seconds: Int = 60) {
        remaining = seconds.toFloat(); speed = 12f; distance = 0f
        stars = 0; boost = 0f; running = true
    }
    fun pause() { running = false }
    fun resume() { if (remaining > 0f) running = true }
    fun reward(countStar: Boolean) {
        if (!running) return
        boost = boostDuration
        if (countStar) stars++
    }
    fun retry() { if (running) boost = 0f }
    fun update(seconds: Float): Boolean {
        if (!running) return false
        val dt = seconds.coerceIn(0f, 0.1f)
        remaining = (remaining - dt).coerceAtLeast(0f)
        boost = (boost - dt).coerceAtLeast(0f)
        val target = if (boost > 0f) boostSpeed else baseSpeed
        speed += (target - speed) * (dt * acceleration).coerceAtMost(1f)
        distance += speed / 3.6f * dt
        if (remaining == 0f) { running = false; speed = 0f; return true }
        return false
    }
}
