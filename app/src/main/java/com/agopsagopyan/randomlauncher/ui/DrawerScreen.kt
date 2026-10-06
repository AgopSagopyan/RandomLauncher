package com.agopsagopyan.randomlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animate
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.agopsagopyan.randomlauncher.data.AppInfo

@Composable
fun DrawerScreen(vm: LauncherViewModel, state: LauncherState, visible: Boolean, onAppLongPress: (AppInfo) -> Unit) {
    var query by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()
    val progress by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "drawer")
    val search = state.config.searchEnabled
    val apps = remember(state.drawer, state.labels, query, search) {
        if (search && query.isNotBlank()) {
            state.drawer.filter { state.label(it).contains(query.trim(), ignoreCase = true) }
        } else {
            state.drawer
        }
    }
    // Reset after closing, while invisible, so the next open starts at the top.
    LaunchedEffect(visible) {
        if (!visible) {
            delay(250)
            query = ""
            gridState.scrollToItem(0)
        }
    }

    // Pull-to-close: once the grid is at the top, leftover downward scroll drags the whole
    // drawer down; releasing past the threshold (or flicking) closes it.
    val scope = rememberCoroutineScope()
    var pull by remember { mutableFloatStateOf(0f) }
    val closeDistance = with(LocalDensity.current) { 96.dp.toPx() }
    val pullToClose = remember(vm) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Scrolling back up while pulled: undo the pull before the grid scrolls.
                if (available.y >= 0f || pull <= 0f) return Offset.Zero
                val used = available.y.coerceAtLeast(-pull)
                pull += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                pull += available.y
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull <= 0f) return Velocity.Zero
                if (pull > closeDistance || available.y > 1500f) {
                    vm.goHome()
                } else {
                    scope.launch { animate(pull, 0f) { v, _ -> pull = v } }
                }
                return available
            }
        }
    }
    LaunchedEffect(visible) { if (visible) pull = 0f }
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)

    Column(
        Modifier
            // Closed: drawn under home so home receives all touches.
            .zIndex(if (visible || progress > 0f) 1f else -1f)
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * size.height / 5f + pull
            }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .blockTouchesBelow()
            .systemBarsPadding(),
    ) {
        // Drag this handle down (or press back/home) to close.
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dy = 0f },
                        onVerticalDrag = { change, amount -> change.consume(); dy += amount },
                        onDragEnd = { if (dy > 60.dp.toPx()) vm.goHome() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
            )
        }

        if (search) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToClose)
                .padding(horizontal = 8.dp),
        ) {
            items(apps, key = { it.id }) { app ->
                AppIcon(
                    app = app,
                    label = state.label(app),
                    showLabel = state.config.showLabels,
                    grayscale = state.config.grayscaleIcons,
                    labelStyle = labelStyle,
                    onClick = { query = ""; vm.open(app) },
                    onLongClick = { onAppLongPress(app) },
                )
            }
        }
    }
}
