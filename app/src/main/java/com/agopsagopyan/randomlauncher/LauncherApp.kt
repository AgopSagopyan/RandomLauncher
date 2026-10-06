package com.agopsagopyan.randomlauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.agopsagopyan.randomlauncher.data.AppRepository
import com.agopsagopyan.randomlauncher.data.ConfigStore
import com.agopsagopyan.randomlauncher.data.ShuffleManager
import com.agopsagopyan.randomlauncher.data.UsageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LauncherApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var store: ConfigStore
        private set
    lateinit var apps: AppRepository
        private set
    lateinit var shuffler: ShuffleManager
        private set
    lateinit var usage: UsageTracker
        private set

    override fun onCreate() {
        super.onCreate()
        store = ConfigStore(this)
        apps = AppRepository(this, scope)
        usage = UsageTracker(this)
        shuffler = ShuffleManager(apps, store, usage)

        // These can't be declared in the manifest since Android 8 and may arrive late for a
        // cached process, so they only raise the pending flag; MainActivity.onStart resolves it.
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    shuffler.markPending(intent.action ?: "broadcast")
                    // Unlock while home is already visible: shuffle right away.
                    if (intent.action == Intent.ACTION_USER_PRESENT) scope.launch { shuffler.shuffle("user present") }
                }
            },
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // Process (re)start usually means boot or a kill; treat it as a fresh unlock.
        shuffler.markPending("process start")
    }
}
