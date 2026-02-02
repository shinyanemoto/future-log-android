package com.shinyanemoto.futurelog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyLogSection(
    val month: YearMonth,
    val entries: List<LogEntry>
)

data class LogUiState(
    val groupedLogs: List<MonthlyLogSection> = emptyList()
)

class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val logsFlow = LogRepository.logs(application)

    val uiState: StateFlow<LogUiState> = logsFlow
        .map { logs ->
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
                MonthlyLogSection(month = month, entries = entries)
            }

            LogUiState(groupedLogs = sections)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LogUiState()
        )

    fun addLog(text: String) {
        viewModelScope.launch {
            LogRepository.addLog(getApplication(), text)
        }
    }
}
