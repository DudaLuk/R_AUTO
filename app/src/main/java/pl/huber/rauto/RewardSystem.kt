package pl.huber.rauto

/** Local, child-friendly progression. It never represents speech correctness by itself. */
class RewardSystem(private val prefs: android.content.SharedPreferences) {
    var points: Int = prefs.getInt("points", 0); private set
    var streak: Int = prefs.getInt("streak", 0); private set
    var bestStreak: Int = prefs.getInt("best_streak", 0); private set
    var boosters: Int = prefs.getInt("boosters", 0); private set
    val level: Int get() = 1 + points / 100
    fun correctAttempt(): Int {
        streak++
        bestStreak = maxOf(bestStreak, streak)
        val bonus = 10 + minOf(streak, 10) * 2
        points += bonus
        if (streak % 5 == 0) boosters++
        persist(); return bonus
    }
    fun missedAttempt() { streak = 0; persist() }
    fun spendBooster(): Boolean { if (boosters <= 0) return false; boosters--; persist(); return true }
    private fun persist() = prefs.edit().putInt("points",points).putInt("streak",streak).putInt("best_streak",bestStreak).putInt("boosters",boosters).apply()
}
