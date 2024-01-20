package org.wordpress.android.stats

import android.content.res.Configuration
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.domains.management.M3Theme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    onBackTapped: () -> Unit,
    onStatsSettingsTapped: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        tabs.size
    }
    val selectedIndex = animateIntAsState(
        targetValue = pagerState.currentPage, label = "selection",
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(id = R.string.stats),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackTapped,
                actions = {
                    IconButton(
                        onClick = onStatsSettingsTapped
                    ) {
                        Icon(
                            Icons.Default.Settings ,
                            contentDescription = stringResource(id = R.string.settings)
                        )
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                TabRow(
                    selectedTabIndex = selectedIndex.value
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            text = { Text(text = tab.title) },
                            selected = index == pagerState.currentPage,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState
                ) {
                    tabs[pagerState.currentPage].screen()
                }
            }
        }
    )
}

@Preview(device = Devices.PIXEL_3A, group = "Initial")
@Preview(device = Devices.PIXEL_3A, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "Initial")
@Composable
fun PreviewStatsScreen() {
    M3Theme {
        StatsScreen(
            onBackTapped = {},
            onStatsSettingsTapped = {},
        )
    }
}

@Preview(device = Devices.PIXEL_3A, group = "Error / Offline")
@Preview(device = Devices.PIXEL_3A, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "Error / Offline")
@Composable
fun PreviewStatsScreenError() {
    M3Theme {
        StatsScreen(
            onBackTapped = {},
            onStatsSettingsTapped = {},
        )
    }
}

@Preview(device = Devices.PIXEL_3A, group = "Empty")
@Preview(device = Devices.PIXEL_3A, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "Empty")
@Composable
fun PreviewStatsScreenEmpty() {
    M3Theme {
        StatsScreen(
            onBackTapped = {},
            onStatsSettingsTapped = {},
        )
    }
}
