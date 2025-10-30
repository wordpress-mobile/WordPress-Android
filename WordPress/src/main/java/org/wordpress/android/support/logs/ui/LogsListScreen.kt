package org.wordpress.android.support.logs.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.support.logs.model.LogDay
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsListScreen(
    logDays: List<LogDay>,
    onLogDayClick: (LogDay) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.support_screen_application_logs_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick
            )
        }
    ) { contentPadding ->
        if (logDays.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.logs_screen_empty_state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                items(
                    items = logDays,
                    key = { it.date }
                ) { logDay ->
                    LogDayListItem(
                        logDay = logDay,
                        onClick = { onLogDayClick(logDay) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogDayListItem(
    logDay: LogDay,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = logDay.displayDate,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.logs_screen_log_count, logDay.logCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_chevron_right_white_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview(showBackground = true, name = "Logs List Screen - Light")
@Composable
private fun LogsListScreenPreview() {
    val exampleList = getExampleLogDaysList()
    AppThemeM3(isDarkTheme = false) {
        LogsListScreen(
            logDays = exampleList,
            onLogDayClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Logs List Screen - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogsListScreenPreviewDark() {
    val exampleList = getExampleLogDaysList()
    AppThemeM3(isDarkTheme = true) {
        LogsListScreen(
            logDays = exampleList,
            onLogDayClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Logs List Screen - Empty")
@Composable
private fun LogsListScreenPreviewEmpty() {
    AppThemeM3(isDarkTheme = false) {
        LogsListScreen(
            logDays = emptyList(),
            onLogDayClick = {},
            onBackClick = {}
        )
    }
}

@Suppress("MagicNumber")
private fun getExampleLogDaysList(): List<LogDay> = listOf(
    LogDay(
        date = "Oct-16",
        displayDate = "October 16",
        logEntries = List(50) { "[Oct-16 12:34:56.789] Sample log entry $it" },
        logCount = 50
    ),
    LogDay(
        date = "Oct-15",
        displayDate = "October 15",
        logEntries = List(32) { "[Oct-15 12:34:56.789] Sample log entry $it" },
        logCount = 32
    ),
    LogDay(
        date = "Oct-14",
        displayDate = "October 14",
        logEntries = List(28) { "[Oct-14 12:34:56.789] Sample log entry $it" },
        logCount = 28
    )
)
