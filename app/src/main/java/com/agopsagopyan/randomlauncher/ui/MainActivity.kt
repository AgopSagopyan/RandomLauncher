package com.agopsagopyan.randomlauncher.ui

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            RandomLauncherTheme(state.config.theme) {
                LauncherRoot(vm, state)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onForeground()
    }

    override fun onStop() {
        super.onStop()
        // Screen turned off while home was in front: next time it shows, it must be reshuffled,
        // and an open drawer shouldn't reorder under the user's eyes after unlock.
        if (!getSystemService(PowerManager::class.java).isInteractive) {
            vm.onScreenOff()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshUsage()
    }

    // Home button while already home: collapse whatever is open.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action) vm.goHome()
    }
}
