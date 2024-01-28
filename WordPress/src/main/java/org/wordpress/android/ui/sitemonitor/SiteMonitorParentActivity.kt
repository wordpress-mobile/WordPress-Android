package org.wordpress.android.ui.sitemonitor

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.sitemonitor.SiteMonitorWebViewClient.SiteMonitorWebViewClientListener
import org.wordpress.android.util.extensions.getSerializableExtraCompat

@AndroidEntryPoint
class SiteMonitorParentActivity: AppCompatActivity(), SiteMonitorWebViewClientListener {
    override fun onWebViewPageLoaded(url: String) = viewModel.onUrlLoaded(url)

    override fun onWebViewReceivedError(url: String) = viewModel.onWebViewError(url)

    val viewModel:SiteMonitorParentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                viewModel.start(getSite())
                SiteMonitorScreen()
            }
        }
    }

    private fun getSite(): SiteModel {
        return requireNotNull(intent.getSerializableExtraCompat(WordPress.SITE)) as SiteModel
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteMonitorScreen(modifier: Modifier = Modifier,
                          viewModel: SiteMonitorParentViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
        val uiStates by viewModel.uiStates.collectAsState()
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.site_monitoring),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            },
            content = {
                TabScreen(modifier = modifier, uiStates)
            }
        )
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun TabScreen(modifier: Modifier = Modifier, uiStates: Map<SiteMonitorType, SiteMonitorUiState>) {
        var tabIndex by remember { mutableIntStateOf(0) }

        val tabs = listOf(
            R.string.site_monitoring_tab_title_metrics,
            R.string.site_monitoring_tab_title_php_logs,
            R.string.site_monitoring_tab_title_web_server_logs
        )

        val tabsToType = mapOf(
            0 to SiteMonitorType.METRICS,
            1 to SiteMonitorType.PHP_LOGS,
            2 to SiteMonitorType.WEB_SERVER_LOGS
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
                        onClick = {
                            tabIndex = index
                            viewModel.onTabSelected(tabsToType[index])
                        }
                    )
                }
            }
            val siteMonitorType = tabsToType[tabIndex] ?: SiteMonitorType.METRICS
            val uiState = uiStates[siteMonitorType] as SiteMonitorUiState
            when(siteMonitorType) {
                SiteMonitorType.METRICS -> SiteMonitoringWebViewForMetric(uiState)
                SiteMonitorType.PHP_LOGS -> SiteMonitoringWebViewForTabPhp(uiState)
                SiteMonitorType.WEB_SERVER_LOGS -> SiteMonitoringWebViewForTabWeb(uiState)
            }
        }
    }

    @Composable
    fun SiteMonitoringWebViewForMetric(uiState: SiteMonitorUiState) {
        LazyColumn {
            item {
                when (uiState) {
                    is SiteMonitorUiState.Preparing -> LoadingState()
                    is SiteMonitorUiState.Prepared, is SiteMonitorUiState.Loaded -> SiteMonitoringWebViewMetric(uiState)
                    is SiteMonitorUiState.Error -> SiteMonitorError(uiState)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun SiteMonitoringWebViewMetric(uiState: SiteMonitorUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }

        if (uiState is SiteMonitorUiState.Prepared) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(this@SiteMonitorParentActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = SiteMonitorWebViewClient(this@SiteMonitorParentActivity)
                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is SiteMonitorUiState.Prepared) {
                LoadingState()
            } else {
                webView?.let { theWebView ->
                    AndroidView(
                        factory = { theWebView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Composable
    fun SiteMonitorError(error: SiteMonitorUiState.Error) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Text(
                text = uiStringText(uiString = error.title),
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiStringText(uiString = error.description),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (error.button != null) {
                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = error.button.click
                ) {
                    Text(text = uiStringText(uiString = error.button.text))
                }
            }
        }
    }
    @Composable
    fun SiteMonitoringWebViewForTabPhp(uiState: SiteMonitorUiState) {
        LazyColumn {
            item {
                when (uiState) {
                    is SiteMonitorUiState.Preparing -> LoadingState()
                    is SiteMonitorUiState.Prepared, is SiteMonitorUiState.Loaded -> SiteMonitoringWebViewPhp(uiState)
                    is SiteMonitorUiState.Error -> SiteMonitorError(uiState)
                }
            }
        }
    }
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun SiteMonitoringWebViewPhp(uiState: SiteMonitorUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }

        if (uiState is SiteMonitorUiState.Prepared) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(this@SiteMonitorParentActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = SiteMonitorWebViewClient(this@SiteMonitorParentActivity)
                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is SiteMonitorUiState.Prepared) {
                LoadingState()
            } else {
                webView?.let { theWebView ->
                    AndroidView(
                        factory = { theWebView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Composable
    fun SiteMonitoringWebViewForTabWeb(uiState: SiteMonitorUiState) {
        LazyColumn {
            item {
                when (uiState) {
                    is SiteMonitorUiState.Preparing -> LoadingState()
                    is SiteMonitorUiState.Prepared, is SiteMonitorUiState.Loaded -> SiteMonitoringWebViewWeb(uiState)
                    is SiteMonitorUiState.Error -> SiteMonitorError(uiState)
                }
            }
        }
    }
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun SiteMonitoringWebViewWeb(uiState: SiteMonitorUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }

        if (uiState is SiteMonitorUiState.Prepared) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(this@SiteMonitorParentActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = SiteMonitorWebViewClient(this@SiteMonitorParentActivity)
                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is SiteMonitorUiState.Prepared) {
                LoadingState()
            } else {
                webView?.let { theWebView ->
                    AndroidView(
                        factory = { theWebView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Composable
    fun LoadingState() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp).fillMaxSize()
            )
            Text(text = "Loading...", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
