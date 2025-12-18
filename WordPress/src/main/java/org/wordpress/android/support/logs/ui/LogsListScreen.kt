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
import org.wordpress.android.support.logs.model.LogFile
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsListScreen(
    logFiles: List<LogFile>,
    onLogFileClick: (LogFile) -> Unit,
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
        if (logFiles.isEmpty()) {
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
                    items = logFiles,
                    key = { it.fileName }
                ) { logFile ->
                    LogFileListItem(
                        logFile = logFile,
                        onClick = { onLogFileClick(logFile) }
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
private fun LogFileListItem(
    logFile: LogFile,
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
                text = logFile.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (logFile.subtitle.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = logFile.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    val exampleList = getExampleLogFilesList()
    AppThemeM3(isDarkTheme = false) {
        LogsListScreen(
            logFiles = exampleList,
            onLogFileClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Logs List Screen - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogsListScreenPreviewDark() {
    val exampleList = getExampleLogFilesList()
    AppThemeM3(isDarkTheme = true) {
        LogsListScreen(
            logFiles = exampleList,
            onLogFileClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Logs List Screen - Empty")
@Composable
private fun LogsListScreenPreviewEmpty() {
    AppThemeM3(isDarkTheme = false) {
        LogsListScreen(
            logFiles = emptyList(),
            onLogFileClick = {},
            onBackClick = {}
        )
    }
}

@Suppress("MagicNumber")
private fun getExampleLogFilesList(): List<LogFile> {
    val mockFile = java.io.File("")
    return listOf(
        LogFile(
            file = mockFile,
            fileName = "2025-11-21T10:42:06+0100.log",
            title = "November 21, 2025",
            subtitle = "10:42 AM",
            logLines = null
        ),
        LogFile(
            file = mockFile,
            fileName = "2025-11-20T14:30:15+0100.log",
            title = "November 20, 2025",
            subtitle = "02:30 PM",
            logLines = null
        ),
        LogFile(
            file = mockFile,
            fileName = "2025-11-19T09:15:42+0100.log",
            title = "November 19, 2025",
            subtitle = "09:15 AM",
            logLines = null
        )
    )
}
