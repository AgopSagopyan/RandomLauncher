package com.agopsagopyan.randomlauncher.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast

object SystemActions {
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        // No public API for this; StatusBarManager#expandNotificationsPanel works with EXPAND_STATUS_BAR.
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    fun lockScreen(context: Context) {
        if (!LockAccessibilityService.lockScreen()) {
            Toast.makeText(context, "Turn on the accessibility service to lock (Settings)", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDefaultClock(context: Context) = start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openAccessibilitySettings(context: Context) = start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openUsageAccessSettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setData("package:${context.packageName}".toUri()),
        fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
    )

    fun openHomeSettings(context: Context) = start(context, Intent(Settings.ACTION_HOME_SETTINGS))

    fun uninstall(context: Context, packageName: String) =
        start(context, Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()))

    private fun start(context: Context, intent: Intent, fallback: Intent? = null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            fallback?.let { f -> runCatching { context.startActivity(f.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
    }
}
