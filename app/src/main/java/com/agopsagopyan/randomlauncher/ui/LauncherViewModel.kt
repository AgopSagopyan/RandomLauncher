package com.agopsagopyan.randomlauncher.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import com.agopsagopyan.randomlauncher.system.LockAccessibilityService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agopsagopyan.randomlauncher.LauncherApp
import com.agopsagopyan.randomlauncher.data.AppInfo
import com.agopsagopyan.randomlauncher.data.LauncherConfig
import com.agopsagopyan.randomlauncher.domain.Friction
import com.agopsagopyan.randomlauncher.domain.FrictionRule
import com.agopsagopyan.randomlauncher.domain.TodayUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data object Drawer : Screen
    data object Settings : Screen
    data class Delay(val app: AppInfo, val seconds: Int, val usage: TodayUsage, val rule: FrictionRule) : Screen
}

sealed interface SettingsPage {
    data object Main : SettingsPage
    data object Appearance : SettingsPage
    data object Dock : SettingsPage
    data object Gestures : SettingsPage
    data object Friction : SettingsPage
    data class FrictionEdit(val appId: String) : SettingsPage
    data object Hidden : SettingsPage
    data object Permissions : SettingsPage
}

@Immutable
data class LauncherState(
    val config: LauncherConfig = LauncherConfig(),
    val allApps: List<AppInfo> = emptyList(),
    /** Display name (renames applied) for every app id. */
    val labels: Map<String, String> = emptyMap(),
    val dock: List<AppInfo?> = emptyList(),
    val drawer: List<AppInfo> = emptyList(),
) {
    fun app(id: String?): AppInfo? = id?.let { i -> allApps.firstOrNull { it.id == i } }
    fun label(app: AppInfo): String = labels[app.id] ?: app.label
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val launcher = application as LauncherApp
    private val store = launcher.store
    private val repo = launcher.apps

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    var usageToday by mutableStateOf<Map<String, TodayUsage>>(emptyMap())
        private set

    var hasUsageAccess by mutableStateOf(false)
        private set

    var settingsPage by mutableStateOf<SettingsPage>(SettingsPage.Main)

    var isDefaultLauncher by mutableStateOf(true)
        private set

    var lockServiceOn by mutableStateOf(false)
        private set

    fun openSettings(page: SettingsPage = SettingsPage.Main) {
        settingsPage = page
        screen = Screen.Settings
    }

    val state: StateFlow<LauncherState> = combine(store.config, repo.apps, store.order) { config, apps, order ->
        val byId = apps.associateBy { it.id }
        val dock = config.dock.take(config.dockSlots).map { byId[it] }.let { slots ->
            slots + List((config.dockSlots - slots.size).coerceAtLeast(0)) { null }
        }
        val rank = order.withIndex().associate { (i, id) -> id to i }
        val drawerIds = launcher.shuffler.drawerIds(apps, config)
        // Apps installed since the last unlock go to the end until the next shuffle.
        val drawer = drawerIds.mapNotNull { byId[it] }.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        LauncherState(
            config = config,
            allApps = apps,
            labels = apps.associate { it.id to (config.renames[it.id] ?: it.label) },
            dock = dock,
            drawer = drawer,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherState())

    fun onForeground() {
        viewModelScope.launch { launcher.shuffler.shuffleIfNeeded() }
    }

    fun onScreenOff() {
        launcher.shuffler.markPending("screen off while home visible")
        screen = Screen.Home
    }

    fun refreshUsage() {
        viewModelScope.launch {
            refreshSystemStatus()
            hasUsageAccess = launcher.usage.hasPermission()
            usageToday = withContext(Dispatchers.IO) { launcher.usage.today() }
        }
    }

    private fun refreshSystemStatus() {
        val app = getApplication<Application>()
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        isDefaultLauncher = app.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == app.packageName
        lockServiceOn = LockAccessibilityService.isRunning
    }

    fun show(target: Screen) {
        screen = target
    }

    fun goHome() {
        settingsPage = SettingsPage.Main
        screen = Screen.Home
    }

    /** Entry point for every app tap: applies friction rules before launching. */
    fun open(app: AppInfo) {
        viewModelScope.launch {
            val rule = state.value.config.friction[app.id]
            if (rule == null || !rule.enabled) {
                launchNow(app)
                return@launch
            }
            val usage = usageFor(app)
            val seconds = Friction.delaySeconds(rule, usage)
            if (seconds <= 0) launchNow(app) else screen = Screen.Delay(app, seconds, usage, rule)
        }
    }

    private suspend fun usageFor(app: AppInfo): TodayUsage {
        hasUsageAccess = launcher.usage.hasPermission()
        if (hasUsageAccess) {
            usageToday = withContext(Dispatchers.IO) { launcher.usage.today() }
            usageToday[app.packageName]?.let { return it }
        }
        return TodayUsage(opens = launcherOpens(app), minutes = 0)
    }

    private suspend fun launcherOpens(app: AppInfo): Int {
        return store.opens.first().counts[app.id] ?: 0
    }

    fun launchNow(app: AppInfo) {
        screen = Screen.Home
        repo.launch(app)
        viewModelScope.launch { store.recordOpen(app.id) }
    }

    fun shuffleNow() {
        viewModelScope.launch { launcher.shuffler.shuffle("manual") }
    }

    fun openAppInfo(app: AppInfo) = repo.openAppInfo(app)

    private fun update(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch { store.updateConfig(transform) }
    }

    fun setDockSlots(slots: Int) = update { c ->
        // Pins that no longer fit fall back into the drawer.
        c.copy(dockSlots = slots, dock = c.dock.take(slots))
    }

    /** Pins to [slot], or to the first free slot. Returns without change when the dock is full. */
    fun pinToDock(app: AppInfo, slot: Int? = null) = update { c ->
        val dock = MutableList(c.dockSlots) { i -> c.dock.getOrNull(i)?.takeIf { it != app.id } ?: "" }
        val target = slot ?: dock.indexOfFirst { it.isEmpty() }
        if (target !in dock.indices) return@update c
        dock[target] = app.id
        c.copy(dock = dock)
    }

    fun unpin(app: AppInfo) = update { c -> c.copy(dock = c.dock.map { if (it == app.id) "" else it }) }

    fun hide(app: AppInfo) = update { c -> c.copy(hidden = c.hidden + app.id, dock = c.dock.map { if (it == app.id) "" else it }) }
    fun unhide(id: String) = update { c -> c.copy(hidden = c.hidden - id) }

    fun rename(app: AppInfo, name: String) = update { c ->
        val trimmed = name.trim()
        c.copy(renames = if (trimmed.isEmpty() || trimmed == app.label) c.renames - app.id else c.renames + (app.id to trimmed))
    }

    fun setFriction(app: AppInfo, rule: FrictionRule?) = update { c ->
        c.copy(friction = if (rule == null) c.friction - app.id else c.friction + (app.id to rule))
    }

    fun setConfig(transform: (LauncherConfig) -> LauncherConfig) = update(transform)
}
