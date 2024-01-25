package org.wordpress.android.ui.sitemonitor

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme

class SiteMonitorParentActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SiteMonitorScreen()
            }
        }
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteMonitorScreen(modifier: Modifier = Modifier) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.site_monitoring),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            },
            content = {
                TabScreen(modifier = modifier)
            }
        )
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun TabScreen(modifier: Modifier = Modifier) {
        var tabIndex by remember { mutableStateOf(0) }

        val tabs = listOf(
            R.string.site_monitoring_tab_title_metrics,
            R.string.site_monitoring_tab_title_php_logs,
            R.string.site_monitoring_tab_title_web_server_logs
        )

        Column(modifier = modifier.fillMaxWidth()) {
            TabRow(
                selectedTabIndex = tabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(stringResource(id = title)) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
            when (tabIndex) {
                0 -> SiteMonitoringWebView()
                1 -> SiteMonitoringWebView()
                2 -> SiteMonitoringWebView()
            }
        }
    }

    @Composable
    fun SiteMonitoringWebView(){
    }
}
