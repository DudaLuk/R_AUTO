package pl.huber.rauto

import android.content.SharedPreferences

/** Persistent garage progression. Upgrades affect only the game feel/visuals, never speech scoring. */
class GarageSystem(private val prefs: SharedPreferences, legacyPoints: Int = 0) {
    enum class Upgrade(val title: String, val maxLevel: Int, val baseCost: Int, val stepCost: Int) {
        ENGINE("Silnik", 5, 60, 45),
        TURBO("Turbo", 5, 50, 40),
        TIRES("Opony", 4, 45, 35),
        BODY("Karoseria", 4, 35, 30)
    }

    var coins: Int = 0
        private set

    init {
        // One-time migration: existing players keep the value of progress already earned.
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            coins = legacyPoints.coerceAtLeast(0)
            prefs.edit()
                .putInt(KEY_COINS, coins)
                .putBoolean(KEY_INITIALIZED, true)
                .apply()
        } else {
            coins = prefs.getInt(KEY_COINS, 0).coerceAtLeast(0)
        }
    }

    fun level(upgrade: Upgrade): Int = prefs.getInt(key(upgrade), 0).coerceIn(0, upgrade.maxLevel)

    fun nextCost(upgrade: Upgrade): Int? {
        val level = level(upgrade)
        if (level >= upgrade.maxLevel) return null
        return upgrade.baseCost + level * upgrade.stepCost
    }

    fun canBuy(upgrade: Upgrade): Boolean {
        val cost = nextCost(upgrade) ?: return false
        return coins >= cost
    }

    fun buy(upgrade: Upgrade): PurchaseResult {
        val current = level(upgrade)
        if (current >= upgrade.maxLevel) return PurchaseResult.MaxLevel
        val cost = nextCost(upgrade) ?: return PurchaseResult.MaxLevel
        if (coins < cost) return PurchaseResult.NotEnough(cost - coins)
        coins -= cost
        prefs.edit()
            .putInt(KEY_COINS, coins)
            .putInt(key(upgrade), current + 1)
            .apply()
        return PurchaseResult.Bought(current + 1, cost)
    }

    fun earn(amount: Int) {
        if (amount <= 0) return
        coins += amount
        prefs.edit().putInt(KEY_COINS, coins).apply()
    }

    val engineLevel get() = level(Upgrade.ENGINE)
    val turboLevel get() = level(Upgrade.TURBO)
    val tiresLevel get() = level(Upgrade.TIRES)
    val bodyLevel get() = level(Upgrade.BODY)

    /** Game tuning derived from upgrades. */
    val baseSpeed: Float get() = 18f + engineLevel * 2.0f
    val boostSpeed: Float get() = 85f + engineLevel * 7.0f
    val boostDuration: Float get() = 3.5f + turboLevel * 0.55f
    val acceleration: Float get() = 2.0f + tiresLevel * 0.35f

    sealed class PurchaseResult {
        data class Bought(val level: Int, val cost: Int) : PurchaseResult()
        data class NotEnough(val missing: Int) : PurchaseResult()
        data object MaxLevel : PurchaseResult()
    }

    private fun key(upgrade: Upgrade) = "garage_${upgrade.name.lowercase()}_level"

    companion object {
        private const val KEY_COINS = "garage_coins"
        private const val KEY_INITIALIZED = "garage_coins_initialized"
    }
}
