package com.agopsagopyan.randomlauncher.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agopsagopyan.randomlauncher.data.AppInfo
import com.agopsagopyan.randomlauncher.data.LauncherConfig
import com.agopsagopyan.randomlauncher.data.ThemeMode
import com.agopsagopyan.randomlauncher.domain.FrictionRule
import com.agopsagopyan.randomlauncher.system.SystemActions

/*
 * Settings are a plain text list in the spirit of the launcher itself: no cards, no chips,
 * no dividers between rows. Hierarchy comes only from type size, weight and opacity.
 */

@Composable
fun SettingsScreen(vm: LauncherViewModel, state: LauncherState) {
    val page = vm.settingsPage
    BackHandler(enabled = page != SettingsPage.Main) {
        vm.settingsPage = if (page is SettingsPage.FrictionEdit) SettingsPage.Friction else SettingsPage.Main
    }

    key(page) {
        when (page) {
            SettingsPage.Main -> MainPage(vm, state)
            SettingsPage.Appearance -> AppearancePage(vm, state)
            SettingsPage.Dock -> DockPage(vm, state)
            SettingsPage.Gestures -> GesturesPage(vm, state)
            SettingsPage.Friction -> FrictionPage(vm, state)
            is SettingsPage.FrictionEdit -> state.app(page.appId)?.let { FrictionEditPage(vm, state, it) }
            SettingsPage.Hidden -> HiddenPage(vm, state)
            SettingsPage.Permissions -> PermissionsPage(vm, state)
        }
    }
}

// region pages

@Composable
private fun MainPage(vm: LauncherViewModel, state: LauncherState) {
    val c = state.config
    val missingPermission = !vm.isDefaultLauncher || !vm.hasUsageAccess ||
        (c.doubleTapLock && !vm.lockServiceOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)

    Page("Settings", onBack = vm::goHome) {
        NavRow("Appearance", "${c.theme.label()} · labels ${if (c.showLabels) "on" else "off"}") {
            vm.settingsPage = SettingsPage.Appearance
        }
        NavRow("Dock", "${c.dockSlots} slots · ${state.dock.count { it != null }} pinned") { vm.settingsPage = SettingsPage.Dock }
        NavRow("Gestures", gestureSummary(state)) { vm.settingsPage = SettingsPage.Gestures }
        NavRow("Wait screen", if (c.friction.isEmpty()) "No rules" else "${c.friction.size} apps") {
            vm.settingsPage = SettingsPage.Friction
        }
        NavRow("Hidden", if (c.hidden.isEmpty()) "None" else "${c.hidden.size} apps") { vm.settingsPage = SettingsPage.Hidden }
        NavRow("Permissions", if (missingPermission) "Something is missing" else "All set", alert = missingPermission) {
            vm.settingsPage = SettingsPage.Permissions
        }
        Spacer(Modifier.height(40.dp))
        TextAction("Shuffle now") { vm.shuffleNow(); vm.goHome() }
    }
}

@Composable
private fun AppearancePage(vm: LauncherViewModel, state: LauncherState) {
    val c = state.config
    Page("Appearance", onBack = { vm.settingsPage = SettingsPage.Main }) {
        ChoiceRow("Theme", ThemeMode.entries, c.theme, { it.label() }) { t -> vm.setConfig { it.copy(theme = t) } }
        ToggleRow("App labels", "Off means icons only", c.showLabels) { v -> vm.setConfig { it.copy(showLabels = v) } }
        ToggleRow("Grayscale icons", "Removes color from app icons", c.grayscaleIcons) { v -> vm.setConfig { it.copy(grayscaleIcons = v) } }
        ToggleRow("Drawer search", "An escape hatch from the shuffle", c.searchEnabled) { v -> vm.setConfig { it.copy(searchEnabled = v) } }
    }
}

@Composable
private fun DockPage(vm: LauncherViewModel, state: LauncherState) {
    var pickSlot by remember { mutableStateOf<Int?>(null) }
    Page("Dock", onBack = { vm.settingsPage = SettingsPage.Main }) {
        ChoiceRow("Slots", LauncherConfig.DOCK_SLOT_OPTIONS, state.config.dockSlots, { it.toString() }) { vm.setDockSlots(it) }
        Caption("Pinned apps")
        state.dock.forEachIndexed { i, app ->
            if (app == null) {
                NavRow("${i + 1}.  Empty", null, dim = true) { pickSlot = i }
            } else {
                AppNavRow(app, state.label(app), null) { pickSlot = i }
            }
        }
    }
    pickSlot?.let { slot ->
        val current = state.dock.getOrNull(slot)
        AppPickerDialog(
            title = "Slot ${slot + 1}",
            apps = state.allApps.filter { it.id !in state.config.hidden },
            state = state,
            onPick = { vm.pinToDock(it, slot) },
            onDismiss = { pickSlot = null },
            onClear = current?.let { app -> { vm.unpin(app) } },
        )
    }
}

private enum class GesturePick { Left, Right, Clock }

@Composable
private fun GesturesPage(vm: LauncherViewModel, state: LauncherState) {
    val c = state.config
    var pick by remember { mutableStateOf<GesturePick?>(null) }
    Page("Gestures", onBack = { vm.settingsPage = SettingsPage.Main }) {
        NavRow("Swipe left", state.app(c.swipeLeftApp)?.let(state::label) ?: "Not set") { pick = GesturePick.Left }
        NavRow("Swipe right", state.app(c.swipeRightApp)?.let(state::label) ?: "Not set") { pick = GesturePick.Right }
        NavRow("Tap clock", state.app(c.clockApp)?.let(state::label) ?: "Clock app") { pick = GesturePick.Clock }
        val lockNote = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> "Needs Android 9+"
            !vm.lockServiceOn -> "Accessibility service is off, see Permissions"
            else -> null
        }
        ToggleRow("Double-tap to lock", lockNote, c.doubleTapLock) { v -> vm.setConfig { it.copy(doubleTapLock = v) } }
        Caption("Always")
        InfoRow("Swipe up", "App drawer")
        InfoRow("Swipe down", "Notifications")
        InfoRow("Long-press empty space", "Settings")
    }

    val apps = state.allApps.filter { it.id !in c.hidden }
    when (pick) {
        GesturePick.Left -> AppPickerDialog("Swipe left", apps, state, onPick = { a -> vm.setConfig { it.copy(swipeLeftApp = a.id) } },
            onDismiss = { pick = null }, onClear = { vm.setConfig { it.copy(swipeLeftApp = null) } })
        GesturePick.Right -> AppPickerDialog("Swipe right", apps, state, onPick = { a -> vm.setConfig { it.copy(swipeRightApp = a.id) } },
            onDismiss = { pick = null }, onClear = { vm.setConfig { it.copy(swipeRightApp = null) } })
        GesturePick.Clock -> AppPickerDialog("Tap clock", apps, state, onPick = { a -> vm.setConfig { it.copy(clockApp = a.id) } },
            onDismiss = { pick = null }, onClear = { vm.setConfig { it.copy(clockApp = null) } })
        null -> Unit
    }
}

@Composable
private fun FrictionPage(vm: LauncherViewModel, state: LauncherState) {
    var adding by remember { mutableStateOf(false) }
    Page("Wait screen", onBack = { vm.settingsPage = SettingsPage.Main }) {
        state.config.friction.forEach { (id, rule) ->
            val app = state.app(id) ?: return@forEach
            AppNavRow(app, state.label(app), rule.summary()) { vm.settingsPage = SettingsPage.FrictionEdit(id) }
        }
        TextAction("+ Add app") { adding = true }

        if (!vm.hasUsageAccess) {
            Spacer(Modifier.height(16.dp))
            Caption("No usage access: only launches from this launcher are counted and time limits don't apply.")
        }

        val top = remember(vm.usageToday, state.allApps) {
            state.allApps
                .mapNotNull { app -> vm.usageToday[app.packageName]?.let { app to it } }
                .filter { it.second.minutes > 0 || it.second.opens > 0 }
                .sortedByDescending { it.second.minutes * 1000 + it.second.opens }
                .take(8)
        }
        if (top.isNotEmpty()) {
            Caption("Today")
            top.forEach { (app, usage) ->
                AppNavRow(app, state.label(app), "${usage.minutes} min · ${usage.opens} opens") {
                    vm.settingsPage = SettingsPage.FrictionEdit(app.id)
                }
            }
        }
    }
    if (adding) {
        AppPickerDialog(
            title = "Add a wait screen to",
            apps = state.allApps.filter { it.id !in state.config.hidden && it.id !in state.config.friction },
            state = state,
            onPick = { app ->
                vm.setFriction(app, FrictionRule())
                vm.settingsPage = SettingsPage.FrictionEdit(app.id)
            },
            onDismiss = { adding = false },
        )
    }
}

/** Edits apply immediately; there is no save button to forget. */
@Composable
fun FrictionEditPage(vm: LauncherViewModel, state: LauncherState, app: AppInfo) {
    val rule = state.config.friction[app.id] ?: FrictionRule(enabled = false)
    fun set(transform: (FrictionRule) -> FrictionRule) = vm.setFriction(app, transform(rule))
    val usage = vm.usageToday[app.packageName]

    Page(state.label(app), onBack = { vm.settingsPage = SettingsPage.Friction }) {
        usage?.let { Caption("Today: ${it.minutes} min · ${it.opens} opens", top = 0.dp) }
        ToggleRow("Wait screen", null, rule.enabled) { v -> set { it.copy(enabled = v) } }
        val dim = !rule.enabled
        StepperRow("Wait", "${rule.baseDelaySec}s", dim,
            onMinus = { set { it.copy(baseDelaySec = stepDown(it.baseDelaySec, 1)) } },
            onPlus = { set { it.copy(baseDelaySec = stepUp(it.baseDelaySec, 60)) } })
        StepperRow("Daily opens", rule.dailyOpenLimit.orNone { "$it×" }, dim,
            onMinus = { set { it.copy(dailyOpenLimit = (it.dailyOpenLimit - 1).coerceAtLeast(0)) } },
            onPlus = { set { it.copy(dailyOpenLimit = (it.dailyOpenLimit + 1).coerceAtMost(200)) } })
        StepperRow("Daily time", rule.dailyMinuteLimit.orNone { "$it min" }, dim,
            onMinus = { set { it.copy(dailyMinuteLimit = (it.dailyMinuteLimit - 5).coerceAtLeast(0)) } },
            onPlus = { set { it.copy(dailyMinuteLimit = (it.dailyMinuteLimit + 5).coerceAtMost(600)) } })
        StepperRow("Over limit", "+${rule.extraDelayPerOverSec}s", dim,
            onMinus = { set { it.copy(extraDelayPerOverSec = (it.extraDelayPerOverSec - 5).coerceAtLeast(0)) } },
            onPlus = { set { it.copy(extraDelayPerOverSec = (it.extraDelayPerOverSec + 5).coerceAtMost(60)) } })

        Caption("Message")
        var message by remember { mutableStateOf(rule.message) }
        BasicTextField(
            value = message,
            onValueChange = { message = it; set { r -> r.copy(message = it.trim()) } },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
            decorationBox = { inner ->
                Column {
                    if (message.isEmpty()) Text("Shown on the wait screen", style = MaterialTheme.typography.bodyLarge, color = soft())
                    inner()
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = soft().copy(alpha = 0.3f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(40.dp))
        TextAction("Delete rule", color = MaterialTheme.colorScheme.error) {
            vm.setFriction(app, null)
            vm.settingsPage = SettingsPage.Friction
        }
    }
}

@Composable
private fun HiddenPage(vm: LauncherViewModel, state: LauncherState) {
    Page("Hidden", onBack = { vm.settingsPage = SettingsPage.Main }) {
        if (state.config.hidden.isEmpty()) Caption("No hidden apps. Long-press an icon to hide it.", top = 0.dp)
        else Caption("Tap to unhide", top = 0.dp)
        state.config.hidden.forEach { id ->
            val app = state.app(id)
            if (app == null) NavRow(id.substringBefore('/'), "Not installed", dim = true) { vm.unhide(id) }
            else AppNavRow(app, state.label(app), null) { vm.unhide(id) }
        }
    }
}

@Composable
private fun PermissionsPage(vm: LauncherViewModel, state: LauncherState) {
    val context = LocalContext.current
    Page("Permissions", onBack = { vm.settingsPage = SettingsPage.Main }) {
        NavRow("Default home app", if (vm.isDefaultLauncher) "Yes" else "No, tap to choose", alert = !vm.isDefaultLauncher) {
            SystemActions.openHomeSettings(context)
        }
        NavRow(
            "Usage access",
            if (vm.hasUsageAccess) "Granted" else "For real screen time and reliable shuffling",
            alert = !vm.hasUsageAccess,
        ) { SystemActions.openUsageAccessSettings(context) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NavRow(
                "Accessibility",
                if (vm.lockServiceOn) "On" else "For double-tap to lock",
                alert = !vm.lockServiceOn && state.config.doubleTapLock,
            ) { SystemActions.openAccessibilitySettings(context) }
        }
    }
}

// endregion

// region building blocks

@Composable
private fun soft(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)

@Composable
private fun Page(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .blockTouchesBelow()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Text(
            "‹",
            fontSize = 32.sp,
            color = soft(),
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 24.dp, bottom = 4.dp),
        )
        Text(title, fontSize = 36.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(28.dp))
        content()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun NavRow(title: String, value: String?, alert: Boolean = false, dim: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Light, color = if (dim) soft() else MaterialTheme.colorScheme.onBackground)
                if (alert) Text("  !", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            value?.let { Text(it, fontSize = 13.sp, color = soft()) }
        }
        Text("›", fontSize = 22.sp, color = soft())
    }
}

@Composable
private fun AppNavRow(app: AppInfo, label: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(app.icon, null, Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
            value?.let { Text(it, fontSize = 13.sp, color = soft()) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, note: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
            note?.let { Text(it, fontSize = 13.sp, color = soft()) }
        }
        Text(
            if (checked) "On" else "Off",
            fontSize = 16.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            color = if (checked) MaterialTheme.colorScheme.onBackground else soft(),
        )
    }
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 6.dp)) {
            options.forEach { option ->
                val on = option == selected
                Text(
                    label(option),
                    fontSize = 16.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.onBackground else soft(),
                    modifier = Modifier
                        .clickable { onSelect(option) }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StepperRow(title: String, value: String, dim: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    val color = if (dim) soft() else MaterialTheme.colorScheme.onBackground
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Light, color = color, modifier = Modifier.weight(1f))
        Text("−", fontSize = 24.sp, color = soft(), modifier = Modifier
            .clickable(onClick = onMinus)
            .padding(horizontal = 14.dp, vertical = 6.dp))
        Text(value, fontSize = 16.sp, color = color, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("+", fontSize = 24.sp, color = soft(), modifier = Modifier
            .clickable(onClick = onPlus)
            .padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Text(title, fontSize = 16.sp, color = soft(), modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, color = soft())
    }
}

@Composable
private fun Caption(text: String, top: androidx.compose.ui.unit.Dp = 24.dp) {
    Text(text, fontSize = 13.sp, color = soft(), modifier = Modifier.padding(top = top, bottom = 4.dp))
}

@Composable
private fun TextAction(text: String, color: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 18.sp,
        color = color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

// endregion

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun gestureSummary(state: LauncherState): String {
    val left = state.app(state.config.swipeLeftApp)?.let(state::label)
    val right = state.app(state.config.swipeRightApp)?.let(state::label)
    return if (left == null && right == null) "No swipes set" else "Left: ${left ?: "—"} · Right: ${right ?: "—"}"
}

private fun FrictionRule.summary() = buildString {
    append(if (enabled) "${baseDelaySec}s" else "Off")
    if (dailyOpenLimit > 0) append(" · $dailyOpenLimit opens")
    if (dailyMinuteLimit > 0) append(" · $dailyMinuteLimit min")
}

private inline fun Int.orNone(format: (Int) -> String) = if (this <= 0) "None" else format(this)

/** 1–10 by one second, then by five. */
private fun stepUp(v: Int, max: Int) = (if (v < 10) v + 1 else v + 5).coerceAtMost(max)
private fun stepDown(v: Int, min: Int) = (if (v <= 10) v - 1 else v - 5).coerceAtLeast(min)
