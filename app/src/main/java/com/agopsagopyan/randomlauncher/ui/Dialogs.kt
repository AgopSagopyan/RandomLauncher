package com.agopsagopyan.randomlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agopsagopyan.randomlauncher.data.AppInfo
import com.agopsagopyan.randomlauncher.system.SystemActions

private enum class MenuMode { Actions, Rename }

@Composable
fun AppMenu(vm: LauncherViewModel, state: LauncherState, app: AppInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(MenuMode.Actions) }
    val inDock = app.id in state.config.dock.take(state.config.dockSlots)
    val dockFull = state.dock.none { it == null }

    when (mode) {
        MenuMode.Rename -> RenameDialog(state.label(app), app.label, onSave = { vm.rename(app, it) }, onDismiss = onDismiss)
        MenuMode.Actions -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Image(app.icon, null, Modifier.size(40.dp)) },
            title = { Text(state.label(app)) },
            text = {
                Column {
                    if (inDock) {
                        MenuItem("Remove from dock") { vm.unpin(app); onDismiss() }
                    } else {
                        MenuItem(if (dockFull) "Pin to dock (dock full)" else "Pin to dock", enabled = !dockFull) {
                            vm.pinToDock(app); onDismiss()
                        }
                    }
                    MenuItem("Rename") { mode = MenuMode.Rename }
                    MenuItem(if (state.config.friction[app.id]?.enabled == true) "Wait screen (on)" else "Wait screen") {
                        vm.openSettings(SettingsPage.FrictionEdit(app.id)); onDismiss()
                    }
                    MenuItem("Hide") { vm.hide(app); onDismiss() }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    MenuItem("App info") { vm.openAppInfo(app); onDismiss() }
                    MenuItem("Uninstall") { SystemActions.uninstall(context, app.packageName); onDismiss() }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
    }
}

@Composable
private fun MenuItem(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun RenameDialog(current: String, original: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
                Text("Leave empty to use the original name ($original).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppInfo>,
    state: LauncherState,
    onPick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    // Pickers are alphabetical on purpose: settings shouldn't fight you, only the drawer should.
    val sorted = remember(apps, state.labels) { apps.sortedBy { state.label(it).lowercase() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(sorted, key = { it.id }) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(app); onDismiss() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(app.icon, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(state.label(app))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = onClear?.let { clear -> { TextButton(onClick = { clear(); onDismiss() }) { Text("Clear") } } },
    )
}
