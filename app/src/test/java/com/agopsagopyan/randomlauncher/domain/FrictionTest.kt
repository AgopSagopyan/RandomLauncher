package com.agopsagopyan.randomlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionTest {
    private val rule = FrictionRule(baseDelaySec = 5, dailyOpenLimit = 3, dailyMinuteLimit = 30, extraDelayPerOverSec = 5)

    @Test
    fun disabledOrMissingRuleHasNoDelay() {
        assertEquals(0, Friction.delaySeconds(null, TodayUsage(100, 100)))
        assertEquals(0, Friction.delaySeconds(rule.copy(enabled = false), TodayUsage(100, 100)))
    }

    @Test
    fun underLimitUsesBaseDelay() {
        assertEquals(5, Friction.delaySeconds(rule, TodayUsage(opens = 2, minutes = 10)))
        assertFalse(Friction.isOverLimit(rule, TodayUsage(2, 10)))
    }

    @Test
    fun delayGrowsWithEachOpenOverLimit() {
        assertEquals(10, Friction.delaySeconds(rule, TodayUsage(opens = 3, minutes = 0)))
        assertEquals(15, Friction.delaySeconds(rule, TodayUsage(opens = 4, minutes = 0)))
        assertTrue(Friction.isOverLimit(rule, TodayUsage(3, 0)))
    }

    @Test
    fun minuteLimitAddsPerTenMinutes() {
        assertEquals(10, Friction.delaySeconds(rule, TodayUsage(opens = 0, minutes = 30)))
        assertEquals(15, Friction.delaySeconds(rule, TodayUsage(opens = 0, minutes = 41)))
    }

    @Test
    fun delayIsCapped() {
        assertEquals(Friction.MAX_DELAY_SEC, Friction.delaySeconds(rule, TodayUsage(opens = 1000, minutes = 0)))
    }
}
