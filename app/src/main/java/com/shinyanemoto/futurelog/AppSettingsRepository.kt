package com.shinyanemoto.futurelog

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsRepository(private val context: Context) {
    private companion object {
        val defaultReminderAmountKey = intPreferencesKey("default_reminder_amount")
        val defaultReminderUnitKey = stringPreferencesKey("default_reminder_unit")
    }

    val defaultReminder: Flow<ReminderOffset?> = context.appSettingsDataStore.data.map { preferences ->
        val amount = preferences[defaultReminderAmountKey]
        val unitName = preferences[defaultReminderUnitKey]
        val unit = unitName?.let { storedValue ->
            ReminderUnit.entries.firstOrNull { it.name == storedValue }
        }

        if (amount == null || unit == null) null else ReminderOffset(amount = amount, unit = unit)
    }

    suspend fun setDefaultReminder(reminderOffset: ReminderOffset?) {
        context.appSettingsDataStore.edit { preferences ->
            if (reminderOffset == null) {
                preferences.remove(defaultReminderAmountKey)
                preferences.remove(defaultReminderUnitKey)
            } else {
                preferences[defaultReminderAmountKey] = reminderOffset.amount
                preferences[defaultReminderUnitKey] = reminderOffset.unit.name
            }
        }
    }
}
