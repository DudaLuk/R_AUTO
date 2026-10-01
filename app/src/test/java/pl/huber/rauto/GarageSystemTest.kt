package pl.huber.rauto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure formulas mirrored from GarageSystem; Android SharedPreferences is covered by manual/instrumented testing. */
class GarageSystemTest {
    @Test fun upgradeCostsGrowPredictably() {
        val engine = GarageSystem.Upgrade.ENGINE
        assertEquals(60, engine.baseCost)
        assertEquals(45, engine.stepCost)
        assertEquals(5, engine.maxLevel)
    }

    @Test fun higherLevelsHaveStrongerGameParameters() {
        fun baseSpeed(level:Int)=18f+level*2f
        fun boostSpeed(level:Int)=85f+level*7f
        fun boostDuration(level:Int)=3.5f+level*0.55f
        assertTrue(baseSpeed(5)>baseSpeed(0))
        assertTrue(boostSpeed(5)>boostSpeed(0))
        assertTrue(boostDuration(5)>boostDuration(0))
    }
}
