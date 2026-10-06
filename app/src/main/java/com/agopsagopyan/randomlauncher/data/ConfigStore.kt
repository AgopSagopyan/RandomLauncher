package com.agopsagopyan.randomlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "launcher")

class ConfigStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val configKey = stringPreferencesKey("config")
    private val orderKey = stringPreferencesKey("drawer_order")
    private val opensKey = stringPreferencesKey("launcher_opens")
    private val lastShuffleKey = longPreferencesKey("last_shuffle_at")

    val config: Flow<LauncherConfig> = context.dataStore.data.map { prefs ->
        prefs[configKey]?.let { runCatching { json.decodeFromString<LauncherConfig>(it) }.getOrNull() } ?: LauncherConfig()
    }

    val order: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[orderKey]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
    }

    val opens: Flow<LauncherOpens> = context.dataStore.data.map { prefs ->
        val stored = prefs[opensKey]?.let { runCatching { json.decodeFromString<LauncherOpens>(it) }.getOrNull() }
        if (stored?.day == today()) stored else LauncherOpens(today())
    }

    suspend fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) {
        context.dataStore.edit { prefs ->
            val current = prefs[configKey]?.let { runCatching { json.decodeFromString<LauncherConfig>(it) }.getOrNull() } ?: LauncherConfig()
            prefs[configKey] = json.encodeToString(transform(current))
        }
    }

    suspend fun currentOrder(): List<String> = order.first()

    suspend fun lastShuffleAt(): Long = context.dataStore.data.first()[lastShuffleKey] ?: 0L

    suspend fun setOrder(order: List<String>) {
        context.dataStore.edit {
            it[orderKey] = json.encodeToString(order)
            it[lastShuffleKey] = System.currentTimeMillis()
        }
    }

    suspend fun recordOpen(id: String) {
        context.dataStore.edit { prefs ->
            val stored = prefs[opensKey]?.let { runCatching { json.decodeFromString<LauncherOpens>(it) }.getOrNull() }
            val base = if (stored?.day == today()) stored else LauncherOpens(today())
            prefs[opensKey] = json.encodeToString(base.copy(counts = base.counts + (id to (base.counts[id] ?: 0) + 1)))
        }
    }

    private fun today() = LocalDate.now().toString()
}
