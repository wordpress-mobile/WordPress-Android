package org.wordpress.android.support.logs.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import org.wordpress.android.R
import org.wordpress.android.support.logs.model.LogDay
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    logDay: LogDay,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = logDay.displayDate,
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShareClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share_white_24dp),
                    contentDescription = stringResource(R.string.reader_btn_share)
                )
            }
        }
    ) { contentPadding ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(
                    items = logDay.logEntries,
                    key = { index, _ -> "${logDay.date}_$index" }
                ) { _, logEntry ->
                    LogEntryItem(logEntry = logEntry)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(logEntry: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Strip HTML tags for display in Compose
        val plainText = HtmlCompat.fromHtml(
            logEntry,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString()

        Text(
            text = plainText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Log Detail Screen - Light")
@Composable
private fun LogDetailScreenPreview() {
    val exampleList = getExampleLogList()
    AppThemeM3(isDarkTheme = false) {
        LogDetailScreen(
            logDay = LogDay(
                date = "Oct-16",
                displayDate = "October 16",
                logEntries = exampleList,
                logCount = exampleList.size
            ),
            onBackClick = {},
            onShareClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Log Detail Screen - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogDetailScreenPreviewDark() {
    val exampleList = getExampleLogList()
    AppThemeM3(isDarkTheme = true) {
        LogDetailScreen(
            logDay = LogDay(
                date = "Oct-16",
                displayDate = "October 16",
                logEntries = exampleList,
                logCount = exampleList.size
            ),
            onBackClick = {},
            onShareClick = {}
        )
    }
}

private fun getExampleLogList(): List<String> = listOf(
    "[Oct-16 12:34:56.789] D/MainActivity: Activity created",
    "[Oct-16 12:34:57.123] I/NetworkManager: Connection established",
    "[Oct-16 12:34:58.456] W/ImageLoader: Cache miss for image_123",
    "[Oct-16 12:35:00.789] E/ApiClient: Request failed with status 404",
    "[Oct-16 12:35:01.234] D/ViewModel: Data loaded successfully",
    "[Oct-16 12:35:02.567] I/Analytics: Event tracked: button_clicked",
    "[Oct-16 12:35:03.890] D/Database: Query executed in 45ms",
    "[Oct-16 12:35:04.123] W/Memory: Low memory warning detected"
)
