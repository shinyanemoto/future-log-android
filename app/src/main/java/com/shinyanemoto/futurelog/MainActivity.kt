package com.shinyanemoto.futurelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shinyanemoto.futurelog.ui.theme.FutureLogTheme
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val createdDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private val presetReminderMonths = listOf(1, 3, 6, 12)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FutureLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FutureLogApp()
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun FutureLogApp() {
    val viewModel: LogViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<LogEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Future Log",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "月ごとのログ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "設定"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                label = { Text(text = "ログを1行で入力") }
            )
            Button(
                onClick = {
                    viewModel.addLog(inputText)
                    inputText = ""
                }
            ) {
                Text(text = "追加")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        MonthlyLogList(
            sections = uiState.groupedLogs,
            modifier = Modifier.weight(1f),
            onEntryClick = { selectedEntry = it }
        )
    }

    if (showSettings) {
        SettingsDialog(
            defaultReminder = uiState.defaultReminder,
            onDismiss = { showSettings = false },
            onSave = { reminder ->
                viewModel.updateDefaultReminder(reminder)
                showSettings = false
            }
        )
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(text = entry.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (entry.createdAt > 0) {
                            val createdDate = Instant.ofEpochMilli(entry.createdAt)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(createdDateFormatter)
                            "登録日: $createdDate"
                        } else {
                            "登録日: 不明"
                        }
                    )
                    Text(
                        text = entry.remindAt?.let {
                            val remindDate = Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(createdDateFormatter)
                            "リマインド日: $remindDate"
                        } ?: "リマインド: OFF"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetReminderMonths.forEach { month ->
                            OutlinedButton(onClick = {
                                viewModel.updateReminder(
                                    entry,
                                    ReminderOffset(month, ReminderUnit.MONTHS)
                                )
                                selectedEntry = null
                            }) {
                                Text("${month}か月")
                            }
                        }
                    }
                    TextButton(onClick = {
                        viewModel.updateReminder(entry, null)
                        selectedEntry = null
                    }) {
                        Text("リマインドを解除")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text("閉じる")
                }
            }
        )
    }
}

@Composable
private fun SettingsDialog(
    defaultReminder: ReminderOffset?,
    onDismiss: () -> Unit,
    onSave: (ReminderOffset?) -> Unit
) {
    var selectedPreset by remember(defaultReminder) {
        mutableStateOf(defaultReminder.takeIf { it != null && it.isPresetMonthReminder() })
    }
    var customAmount by remember(defaultReminder) {
        mutableStateOf(
            defaultReminder
                ?.takeUnless { it.isPresetMonthReminder() }
                ?.amount
                ?.toString()
                .orEmpty()
        )
    }
    var customUnit by remember(defaultReminder) {
        mutableStateOf(
            defaultReminder
                ?.takeUnless { it.isPresetMonthReminder() }
                ?.unit
                ?: ReminderUnit.MONTHS
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "新規ログの既定リマインド",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "現在: ${formatReminderLabel(defaultReminder)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                ReminderSettingsEditor(
                    selectedPreset = selectedPreset,
                    onPresetSelected = {
                        selectedPreset = it
                        customAmount = ""
                    },
                    customAmount = customAmount,
                    onCustomAmountChange = {
                        customAmount = it
                        if (it.isNotBlank()) selectedPreset = null
                    },
                    customUnit = customUnit,
                    onCustomUnitChange = { customUnit = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val customReminder = customAmount.toIntOrNull()?.takeIf { it > 0 }?.let {
                        ReminderOffset(amount = it, unit = customUnit)
                    }
                    onSave(customReminder ?: selectedPreset)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun MonthlyLogList(
    sections: List<MonthlyLogSection>,
    modifier: Modifier = Modifier,
    onEntryClick: (LogEntry) -> Unit
) {
    if (sections.isEmpty()) {
        Text(
            text = "まだログがありません。",
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sections.forEach { section ->
            item(key = section.month) {
                Column {
                    Text(
                        text = section.month.format(DateTimeFormatter.ofPattern("yyyy年 M月")),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(section.entries, key = { "${it.id}-${it.timestamp}-${it.text}" }) { entry ->
                Text(
                    text = "・${entry.text}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onEntryClick(entry) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (section.recommendedEntries.isNotEmpty()) {
                item(key = "recommend-${section.month}") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "おすすめ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                items(section.recommendedEntries, key = { "recommend-${it.id}-${section.month}" }) { entry ->
                    Text(
                        text = "・${entry.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { onEntryClick(entry) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ReminderSettingsEditor(
    selectedPreset: ReminderOffset?,
    onPresetSelected: (ReminderOffset?) -> Unit,
    customAmount: String,
    onCustomAmountChange: (String) -> Unit,
    customUnit: ReminderUnit,
    onCustomUnitChange: (ReminderUnit) -> Unit
) {
    val presets = listOf(
        null,
        ReminderOffset(1, ReminderUnit.MONTHS),
        ReminderOffset(3, ReminderUnit.MONTHS),
        ReminderOffset(6, ReminderUnit.MONTHS),
        ReminderOffset(12, ReminderUnit.MONTHS)
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            OutlinedButton(onClick = { onPresetSelected(preset) }) {
                val label = when {
                    preset == null -> "なし"
                    preset.unit == ReminderUnit.MONTHS -> "${preset.amount}か月"
                    else -> "${preset.amount}日"
                }
                Text(text = if (selectedPreset == preset) "✓$label" else label)
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = customAmount,
            onValueChange = onCustomAmountChange,
            modifier = Modifier.weight(1f),
            label = { Text("カスタム") }
        )
        OutlinedButton(onClick = { onCustomUnitChange(ReminderUnit.MONTHS) }) {
            Text(if (customUnit == ReminderUnit.MONTHS) "✓月" else "月")
        }
        OutlinedButton(onClick = { onCustomUnitChange(ReminderUnit.DAYS) }) {
            Text(if (customUnit == ReminderUnit.DAYS) "✓日" else "日")
        }
    }
}

private fun ReminderOffset.isPresetMonthReminder(): Boolean =
    unit == ReminderUnit.MONTHS && amount in presetReminderMonths

private fun formatReminderLabel(reminder: ReminderOffset?): String {
    if (reminder == null) return "なし"

    return when (reminder.unit) {
        ReminderUnit.MONTHS -> "${reminder.amount}か月"
        ReminderUnit.DAYS -> "${reminder.amount}日"
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FutureLogTheme {
        Column(modifier = Modifier.padding(24.dp)) {
            val previewLogs = listOf(
                LogEntry(text = "歯医者に行った", timestamp = Instant.now().minusSeconds(86_400).toEpochMilli()),
                LogEntry(text = "お風呂の洗剤を買った", timestamp = Instant.now().toEpochMilli())
            )
            val previewSections = listOf(
                MonthlyLogSection(
                    month = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault())),
                    entries = previewLogs,
                    recommendedEntries = previewLogs
                )
            )
            MonthlyLogList(sections = previewSections, onEntryClick = {})
        }
    }
}
