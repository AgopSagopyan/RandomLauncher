package com.agopsagopyan.randomlauncher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agopsagopyan.randomlauncher.data.AppInfo
import com.agopsagopyan.randomlauncher.system.SystemActions
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    state: LauncherState,
    onAppLongPress: (AppInfo) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
) {
    val context = LocalContext.current
    val config = state.config

    // Swipes are read by the outer box (sees every pointer, even over icons).
    // Taps live on a background layer so long-pressing an icon never also opens settings.
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(state) {
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); total += amount },
                    onDragEnd = {
                        val threshold = 120.dp.toPx()
                        val (dx, dy) = total
                        when {
                            abs(dy) > abs(dx) && dy < -threshold -> vm.show(Screen.Drawer)
                            abs(dy) > abs(dx) && dy > threshold -> SystemActions.expandNotifications(context)
                            abs(dx) > abs(dy) && dx > threshold -> state.app(config.swipeRightApp)?.let(vm::open)
                            abs(dx) > abs(dy) && dx < -threshold -> state.app(config.swipeLeftApp)?.let(vm::open)
                        }
                    },
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(config.doubleTapLock) {
                    detectTapGestures(
                        onDoubleTap = { if (config.doubleTapLock) SystemActions.lockScreen(context) },
                        onLongPress = { vm.openSettings() },
                    )
                },
        )
        HomeContent(vm, state, onAppLongPress, onEmptySlotLongPress)
    }
}

@Composable
private fun HomeContent(
    vm: LauncherViewModel,
    state: LauncherState,
    onAppLongPress: (AppInfo) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
) {
    val context = LocalContext.current
    val config = state.config
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Clock(onClick = {
            state.app(config.clockApp)?.let(vm::open) ?: SystemActions.openDefaultClock(context)
        })
        Spacer(Modifier.weight(1f))
        Dock(state, vm, onAppLongPress, onEmptySlotLongPress)
        Text(
            text = "︿",
            style = OnWallpaper.copy(fontSize = 14.sp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Clock(onClick: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    Column(
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Text(now.format(DateTimeFormatter.ofPattern(timePattern)), style = OnWallpaper.copy(fontSize = 64.sp, fontWeight = FontWeight.Light))
        Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)), style = OnWallpaper.copy(fontSize = 18.sp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(
    state: LauncherState,
    vm: LauncherViewModel,
    onAppLongPress: (AppInfo) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        state.dock.forEachIndexed { slot, app ->
            if (app != null) {
                AppIcon(
                    app = app,
                    label = state.label(app),
                    showLabel = state.config.showLabels,
                    grayscale = state.config.grayscaleIcons,
                    labelStyle = OnWallpaper,
                    onClick = { vm.open(app) },
                    onLongClick = { onAppLongPress(app) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(
                    Modifier
                        .weight(1f)
                        .height(if (state.config.showLabels) 84.dp else 68.dp)
                        .combinedClickable(onClick = {}, onLongClick = { onEmptySlotLongPress(slot) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", style = OnWallpaper.copy(fontSize = 20.sp, color = OnWallpaper.color.copy(alpha = 0.35f)))
                }
            }
        }
    }
}
