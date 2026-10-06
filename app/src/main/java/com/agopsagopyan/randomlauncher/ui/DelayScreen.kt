package com.agopsagopyan.randomlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agopsagopyan.randomlauncher.domain.Friction
import kotlinx.coroutines.delay

/** Full-screen pause before a rule-bound app opens. The countdown must finish, then you still have to tap "Open anyway". */
@Composable
fun DelayScreen(vm: LauncherViewModel, state: LauncherState, delay: Screen.Delay) {
    var remaining by remember(delay) { mutableIntStateOf(delay.seconds) }
    LaunchedEffect(delay) {
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }
    val app = delay.app
    val over = Friction.isOverLimit(delay.rule, delay.usage)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Swallow touches so nothing underneath reacts.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(app.icon, null, Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(state.label(app), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        val usageText = buildString {
            append("Opened ${delay.usage.opens}× today")
            if (vm.hasUsageAccess) append(" · ${delay.usage.minutes} min")
        }
        Text(usageText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (over) {
            Text("Daily limit reached, so the wait is longer.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }
        if (delay.rule.message.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text(delay.rule.message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(progress = { 1f - remaining.toFloat() / delay.seconds })
        Spacer(Modifier.height(8.dp))
        Text(if (remaining > 0) "${remaining}s" else "Ready", color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { vm.launchNow(app) }, enabled = remaining == 0) { Text("Open anyway") }
        OutlinedButton(onClick = { vm.goHome() }, modifier = Modifier.padding(top = 8.dp)) { Text("Cancel") }
    }
}
