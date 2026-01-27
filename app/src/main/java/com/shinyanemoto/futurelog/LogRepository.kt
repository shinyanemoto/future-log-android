package com.shinyanemoto.futurelog

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.logDataStore: DataStore<Preferences> by preferencesDataStore(name = "future_log_entries")

private val logEntriesKey = preferencesKey<String>("log_entries_json")

object LogRepository {
    fun logs(context: Context): Flow<List<LogEntry>> {
        return context.logDataStore.data.map { preferences ->
            val json = preferences[logEntriesKey].orEmpty()
            parseEntries(json)
        }
    }

    suspend fun addLog(context: Context, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        context.logDataStore.edit { preferences ->
            val current = parseEntries(preferences[logEntriesKey].orEmpty()).toMutableList()
            current.add(0, LogEntry(text = trimmed, timestamp = System.currentTimeMillis()))
            preferences[logEntriesKey] = serializeEntries(current)
        }
    }

    private fun parseEntries(json: String): List<LogEntry> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                LogEntry(
                    text = obj.getString("text"),
                    timestamp = obj.getLong("timestamp")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeEntries(entries: List<LogEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
                .put("text", entry.text)
                .put("timestamp", entry.timestamp)
            array.put(obj)
        }
        return array.toString()
    }
}

data class LogEntry(
    val text: String,
    val timestamp: Long
)
