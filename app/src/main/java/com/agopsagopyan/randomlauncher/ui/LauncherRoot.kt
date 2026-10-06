package com.agopsagopyan.randomlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.agopsagopyan.randomlauncher.data.AppInfo

@Composable
fun LauncherRoot(vm: LauncherViewModel, state: LauncherState) {
    val screen = vm.screen
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    var pickSlot by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = screen != Screen.Home) { vm.goHome() }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            vm = vm,
            state = state,
            onAppLongPress = { menuFor = it },
            onEmptySlotLongPress = { pickSlot = it },
        )

        // Always composed (just faded/offset and pushed below home when closed) so opening
        // it never pays for composing the grid.
        DrawerScreen(vm = vm, state = state, visible = screen == Screen.Drawer, onAppLongPress = { menuFor = it })

        AnimatedVisibility(
            visible = screen == Screen.Settings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(2f),
        ) {
            SettingsScreen(vm = vm, state = state)
        }

        (screen as? Screen.Delay)?.let { Box(Modifier.zIndex(2f)) { DelayScreen(vm = vm, state = state, delay = it) } }
    }

    menuFor?.let { app -> AppMenu(vm = vm, state = state, app = app, onDismiss = { menuFor = null }) }

    pickSlot?.let { slot ->
        AppPickerDialog(
            title = "Choose an app for the dock",
            apps = state.allApps.filter { it.id !in state.config.hidden },
            state = state,
            onPick = { vm.pinToDock(it, slot) },
            onDismiss = { pickSlot = null },
        )
    }
}
