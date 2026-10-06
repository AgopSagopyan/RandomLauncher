package com.agopsagopyan.randomlauncher.data

import com.agopsagopyan.randomlauncher.domain.FrictionRule
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything the user can configure. App references are [AppInfo.id] strings. */
@Serializable
@Immutable
data class LauncherConfig(
    val dockSlots: Int = 4,
    val dock: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val renames: Map<String, String> = emptyMap(),
    val showLabels: Boolean = true,
    val grayscaleIcons: Boolean = false,
    val searchEnabled: Boolean = false,
    val swipeLeftApp: String? = null,
    val swipeRightApp: String? = null,
    val clockApp: String? = null,
    val doubleTapLock: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val friction: Map<String, FrictionRule> = emptyMap(),
) {
    companion object {
        val DOCK_SLOT_OPTIONS = listOf(1, 2, 3, 4, 5)
    }
}

/** Opens made through the launcher today; fallback when usage access is not granted. */
@Serializable
data class LauncherOpens(val day: String = "", val counts: Map<String, Int> = emptyMap())
