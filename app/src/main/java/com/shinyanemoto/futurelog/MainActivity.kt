package com.shinyanemoto.futurelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shinyanemoto.futurelog.ui.theme.FutureLogTheme
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

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
fun FutureLogApp() {
    val context = LocalContext.current
    val logs by LogRepository.logs(context).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
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
                    val current = inputText
                    scope.launch {
                        LogRepository.addLog(context, current)
                    }
                    inputText = ""
                }
            ) {
                Text(text = "追加")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        MonthlyLogList(
            logs = logs,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MonthlyLogList(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    val grouped = logs
        .groupBy { entry ->
            YearMonth.from(Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()))
        }
        .toSortedMap(compareByDescending { it })

    if (grouped.isEmpty()) {
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
        grouped.forEach { (month, entries) ->
            item(key = month) {
                Column {
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("yyyy年 M月")),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(entries, key = { "${it.timestamp}-${it.text}" }) { entry ->
                Text(
                    text = "・${entry.text}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FutureLogTheme {
        Column(modifier = Modifier.padding(24.dp)) {
            val previewLogs = listOf(
                LogEntry("歯医者に行った", Instant.now().minusSeconds(86_400).toEpochMilli()),
                LogEntry("お風呂の洗剤を買った", Instant.now().toEpochMilli())
            )
            MonthlyLogList(logs = previewLogs)
        }
    }
}
