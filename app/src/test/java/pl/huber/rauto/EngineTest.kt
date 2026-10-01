package pl.huber.rauto

import org.junit.Assert.*
import org.junit.Test

class EngineTest {
    @Test fun pauseFreezesClockAndDistance() {
        val e=RaceEngine();e.start();e.update(0.1f);e.pause()
        val remaining=e.remaining;val distance=e.distance
        e.update(0.1f);e.reward(true)
        assertEquals(remaining,e.remaining,0f);assertEquals(distance,e.distance,0f)
        assertEquals(0,e.stars)
    }
    @Test fun laboratoryDoesNotEarnStars() {
        val e=RaceEngine();e.start();e.reward(false)
        repeat(10) { e.update(0.1f) }
        assertEquals(0,e.stars);assertTrue(e.speed>40f)
    }
    @Test fun retryGraduallySlowsCarAndKeepsStars() {
        val e=RaceEngine();e.start();e.reward(true)
        repeat(10) { e.update(0.1f) }
        val fast=e.speed;e.retry();e.update(0.1f)
        assertTrue(e.speed<fast);assertTrue(e.speed>18f);assertEquals(1,e.stars)
    }
    @Test fun finishFiresOnce() {
        val e=RaceEngine();e.start(1)
        var finishes=0
        repeat(30) { if(e.update(0.1f)) finishes++ }
        assertEquals(1,finishes);assertEquals(0f,e.remaining,0f);assertFalse(e.running)
        e.resume();assertFalse(e.running)
    }
    @Test fun soundNeedsDurationAndSilenceBeforeAnotherTrigger() {
        val gate=SoundGate()
        repeat(3) { assertFalse(gate.accept(-20.0,-35.0,50)) }
        assertTrue(gate.accept(-20.0,-35.0,50))
        repeat(20) { assertFalse(gate.accept(-20.0,-35.0,50)) }
        repeat(8) { assertFalse(gate.accept(-60.0,-35.0,50)) }
        repeat(3) { assertFalse(gate.accept(-20.0,-35.0,50)) }
        assertTrue(gate.accept(-20.0,-35.0,50))
    }
    @Test fun isolatedClicksAndSilenceDoNotTrigger() {
        val gate=SoundGate()
        repeat(40) {
            assertFalse(gate.accept(-10.0,-35.0,50))
            assertFalse(gate.accept(-80.0,-35.0,50))
        }
    }
}
