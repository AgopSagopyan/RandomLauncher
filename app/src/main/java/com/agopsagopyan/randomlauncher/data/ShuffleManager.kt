package com.agopsagopyan.randomlauncher.data

import android.util.Log
import com.agopsagopyan.randomlauncher.domain.Shuffler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Decides when to reshuffle. Broadcasts alone are unreliable (Android 14+ defers them for
 * cached processes, some OEMs skip USER_PRESENT on swipe-only lock), so several signals feed
 * a single "pending" flag that is resolved whenever the launcher comes to the foreground.
 */
class ShuffleManager(
    private val apps: AppRepository,
    private val store: ConfigStore,
    private val usage: UsageTracker,
) {
    private val mutex = Mutex()

    @Volatile
    private var pending = false

    /** Ids that live in the drawer (not hidden, not in the visible dock). */
    fun drawerIds(all: List<AppInfo>, config: LauncherConfig): List<String> {
        val docked = config.dock.take(config.dockSlots).toSet()
        return all.map { it.id }.filter { it !in config.hidden && it !in docked }
    }

    fun markPending(reason: String) {
        if (!pending) Log.d(TAG, "shuffle pending: $reason")
        pending = true
    }

    /** Called when home becomes visible; shuffles if the device was locked/screen-off since last time. */
    suspend fun shuffleIfNeeded() {
        val reason = when {
            pending -> "pending flag"
            withContext(Dispatchers.IO) { usage.unlockedSince(store.lastShuffleAt()) } -> "usage events"
            else -> return
        }
        shuffle(reason)
    }

    suspend fun shuffle(reason: String) = mutex.withLock {
        withTimeoutOrNull(10_000) { apps.loaded.first { it } } ?: run {
            Log.w(TAG, "shuffle skipped ($reason): apps not loaded")
            return@withLock
        }
        val ids = drawerIds(apps.apps.value, store.config.first())
        store.setOrder(Shuffler.shuffle(ids, store.currentOrder()))
        pending = false
        Log.d(TAG, "shuffled ${ids.size} apps ($reason)")
    }

    companion object {
        const val TAG = "RandomLauncher"
    }
}
