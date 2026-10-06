package com.agopsagopyan.randomlauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.agopsagopyan.randomlauncher.domain.TodayUsage
import java.time.LocalDate
import java.time.ZoneId

/** Reads real per-app usage for today from the system (needs "Usage access"). */
class UsageTracker(private val context: Context) {
    private val usm = context.getSystemService(UsageStatsManager::class.java)

    @Suppress("DEPRECATION")
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Per package: number of times it came to the foreground today and foreground minutes. */
    fun today(): Map<String, TodayUsage> {
        if (!hasPermission()) return emptyMap()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val opens = HashMap<String, Int>()
        val foregroundMs = HashMap<String, Long>()
        val resumedAt = HashMap<String, Long>()
        var lastForeground: String? = null

        val events = usm.queryEvents(start, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Count a new "open" only when switching from another package.
                    if (pkg != lastForeground) opens[pkg] = (opens[pkg] ?: 0) + 1
                    lastForeground = pkg
                    resumedAt.putIfAbsent(pkg, event.timeStamp)
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    resumedAt.remove(pkg)?.let { foregroundMs[pkg] = (foregroundMs[pkg] ?: 0) + (event.timeStamp - it) }
                }
                SCREEN_NON_INTERACTIVE -> {
                    // Screen off: close any open session and reset so the next resume counts as an open.
                    resumedAt.forEach { (p, t) -> foregroundMs[p] = (foregroundMs[p] ?: 0) + (event.timeStamp - t) }
                    resumedAt.clear()
                    lastForeground = null
                }
            }
        }
        resumedAt.forEach { (p, t) -> foregroundMs[p] = (foregroundMs[p] ?: 0) + (now - t) }

        return (opens.keys + foregroundMs.keys).associateWith {
            TodayUsage(opens = opens[it] ?: 0, minutes = ((foregroundMs[it] ?: 0) / 60_000).toInt())
        }
    }

    /** True if the keyguard was dismissed or the screen turned on after [since]. Needs usage access. */
    fun unlockedSince(since: Long): Boolean {
        if (since <= 0L || !hasPermission()) return false
        val events = usm.queryEvents(since, System.currentTimeMillis())
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == KEYGUARD_HIDDEN || event.eventType == SCREEN_INTERACTIVE) return true
        }
        return false
    }

    private companion object {
        // UsageEvents.Event constants added in API 28; plain ints so lint stays quiet on 26/27,
        // where these events simply never appear.
        const val SCREEN_INTERACTIVE = 15
        const val KEYGUARD_HIDDEN = 18
        const val SCREEN_NON_INTERACTIVE = 16
    }
}
