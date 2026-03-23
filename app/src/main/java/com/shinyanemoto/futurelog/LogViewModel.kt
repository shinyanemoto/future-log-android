package com.shinyanemoto.futurelog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyLogSection(
    val month: YearMonth,
    val entries: List<LogEntry>,
    val recommendedEntries: List<LogEntry>
)

data class LogUiState(
    val groupedLogs: List<MonthlyLogSection> = emptyList(),
    val defaultReminder: ReminderOffset? = null
)

class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val logsFlow = LogRepository.logs(application)
    private val settingsRepository = AppSettingsRepository(application)
    private val defaultReminderFlow = settingsRepository.defaultReminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<LogUiState> = logsFlow
        .combine(defaultReminderFlow) { logs, defaultReminder ->
            val grouped = logs
                .sortedByDescending { it.timestamp }
                .groupBy { entry ->
                    YearMonth.from(
                        Instant.ofEpochMilli(entry.timestamp)
                            .atZone(ZoneId.systemDefault())
                    )
                }
                .toSortedMap(compareByDescending { it })

            val sections = grouped.map { (month, entries) ->
                val monthEnd = month.atEndOfMonth().atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val recommendedEntries = logs
                    .asSequence()
                    .filter { it.remindAt != null && it.remindAt <= monthEnd }
                    .sortedByDescending { it.remindAt }
                    .toList()
                MonthlyLogSection(
                    month = month,
                    entries = entries,
                    recommendedEntries = recommendedEntries
                )
            }

            LogUiState(
                groupedLogs = sections,
                defaultReminder = defaultReminder
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LogUiState()
        )

    fun addLog(text: String) {
        viewModelScope.launch {
            val remindAt = defaultReminderFlow.value?.let {
                calculateRemindAt(createdAt = System.currentTimeMillis(), offset = it)
            }
            LogRepository.addLog(getApplication(), text, remindAt)
        }
    }

    fun updateReminder(entry: LogEntry, reminderOffset: ReminderOffset?) {
        viewModelScope.launch {
            val base = if (entry.createdAt > 0) entry.createdAt else System.currentTimeMillis()
            val remindAt = reminderOffset?.let { calculateRemindAt(base, it) }
            LogRepository.updateReminder(getApplication(), entry.id, remindAt)
        }
    }

    fun updateMemo(entry: LogEntry, memo: String) {
        viewModelScope.launch {
            LogRepository.updateMemo(getApplication(), entry.id, memo)
        }
    }

    fun updateDefaultReminder(reminderOffset: ReminderOffset?) {
        viewModelScope.launch {
            settingsRepository.setDefaultReminder(reminderOffset)
        }
    }

    private fun calculateRemindAt(createdAt: Long, offset: ReminderOffset): Long {
        val zone = ZoneId.systemDefault()
        val baseDateTime = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDateTime()
        val remindDateTime = when (offset.unit) {
            ReminderUnit.MONTHS -> baseDateTime.plusMonths(offset.amount.toLong())
            ReminderUnit.DAYS -> baseDateTime.plusDays(offset.amount.toLong())
        }
        return remindDateTime.atZone(zone).toInstant().toEpochMilli()
    }
}

enum class ReminderUnit { MONTHS, DAYS }

data class ReminderOffset(
    val amount: Int,
    val unit: ReminderUnit
)
