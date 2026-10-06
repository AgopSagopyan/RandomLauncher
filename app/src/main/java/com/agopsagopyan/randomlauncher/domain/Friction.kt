package com.agopsagopyan.randomlauncher.domain

import kotlinx.serialization.Serializable

@Serializable
data class FrictionRule(
    val enabled: Boolean = true,
    val baseDelaySec: Int = 5,
    /** 0 = no limit */
    val dailyOpenLimit: Int = 0,
    /** 0 = no limit */
    val dailyMinuteLimit: Int = 0,
    /** Added for every open beyond the limit (and once per started 10 min beyond the minute limit). */
    val extraDelayPerOverSec: Int = 5,
    val message: String = "",
)

data class TodayUsage(val opens: Int, val minutes: Int)

object Friction {
    const val MAX_DELAY_SEC = 300

    fun delaySeconds(rule: FrictionRule?, usage: TodayUsage): Int {
        if (rule == null || !rule.enabled) return 0
        var delay = rule.baseDelaySec
        if (rule.dailyOpenLimit > 0 && usage.opens >= rule.dailyOpenLimit) {
            delay += (usage.opens - rule.dailyOpenLimit + 1) * rule.extraDelayPerOverSec
        }
        if (rule.dailyMinuteLimit > 0 && usage.minutes >= rule.dailyMinuteLimit) {
            delay += ((usage.minutes - rule.dailyMinuteLimit) / 10 + 1) * rule.extraDelayPerOverSec
        }
        return delay.coerceIn(0, MAX_DELAY_SEC)
    }

    fun isOverLimit(rule: FrictionRule?, usage: TodayUsage): Boolean {
        if (rule == null || !rule.enabled) return false
        return (rule.dailyOpenLimit > 0 && usage.opens >= rule.dailyOpenLimit) ||
            (rule.dailyMinuteLimit > 0 && usage.minutes >= rule.dailyMinuteLimit)
    }
}
