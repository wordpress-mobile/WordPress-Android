package org.wordpress.android.ui.sitemonitor

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SiteMonitorTabHeader(navController: (String) -> Unit) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        SiteMonitorTabItem.Metrics,
        SiteMonitorTabItem.PHPLogs,
        SiteMonitorTabItem.WebServerLogs
    )
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        indicator = { tabPositions ->
            // Customizing the indicator color and style
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colors.onSurface,
                height = 2.0.dp
            )
        }
    ) {
        tabs.forEachIndexed { index, item ->
            Tab(
                text = {
                    Column (horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(item.title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                selected = selectedTabIndex == index,
                onClick = {
                    selectedTabIndex = index
                    navController(item.route)
                },
            )
        }
    }
}
