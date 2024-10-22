package org.wordpress.android.ui.sitemonitor

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

@SuppressLint("SetJavaScriptEnabled")
@AndroidEntryPoint
class SiteMonitorParentActivity : AppCompatActivity(), SiteMonitorWebViewClient.SiteMonitorWebViewClientListener {
    @Inject
    lateinit var siteMonitorUtils: SiteMonitorUtils

    private var savedStateSparseArray = SparseArray<Fragment.SavedState>()
    private var currentSelectItemId = 0

    private val siteMonitorParentViewModel: SiteMonitorParentViewModel by viewModels()

    private val metricsWebView by lazy {
        commonWebView(SiteMonitorType.METRICS)
    }

    private val phpLogsWebView by lazy {
        commonWebView(SiteMonitorType.PHP_LOGS)
    }

    private val webServerLogsWebView by lazy {
        commonWebView(SiteMonitorType.WEB_SERVER_LOGS)
    }

    private fun commonWebView(
        siteMonitorType: SiteMonitorType
    ) = WebView(this@SiteMonitorParentActivity).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        settings.userAgentString = siteMonitorUtils.getUserAgent()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        webViewClient = SiteMonitorWebViewClient(this@SiteMonitorParentActivity, siteMonitorType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // not sure about this one, double check if this works as expected
            settings.isAlgorithmicDarkeningAllowed = true
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            savedStateSparseArray = savedInstanceState.getSparseParcelableArray(
                SAVED_STATE_CONTAINER_KEY
            )
                ?: savedStateSparseArray
            currentSelectItemId = savedInstanceState.getInt(SAVED_STATE_CURRENT_TAB_KEY)
            siteMonitorParentViewModel.loadData()
        } else {
            siteMonitorParentViewModel.start(getSite())
            currentSelectItemId = getInitialTab()
        }
        setContent {
            AppThemeM2 {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SiteMonitorScreen(initialTab = currentSelectItemId)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSparseParcelableArray(SAVED_STATE_CONTAINER_KEY, savedStateSparseArray)
        outState.putInt(SAVED_STATE_CURRENT_TAB_KEY, currentSelectItemId)
    }

    private fun getSite(): SiteModel {
        return requireNotNull(intent.getSerializableExtraCompat(WordPress.SITE)) as SiteModel
    }

    private fun getInitialTab(): Int {
        val tab = intent?.getSerializableExtraCompat(ARG_SITE_MONITOR_TYPE_KEY) as SiteMonitorType?
            ?: SiteMonitorType.METRICS
        return when (tab) {
            SiteMonitorType.METRICS -> 0
            SiteMonitorType.PHP_LOGS -> 1
            SiteMonitorType.WEB_SERVER_LOGS -> 2
        }
    }

    companion object {
        const val ARG_SITE_MONITOR_TYPE_KEY = "ARG_SITE_MONITOR_TYPE_KEY"
        const val SAVED_STATE_CONTAINER_KEY = "ContainerKey"
        const val SAVED_STATE_CURRENT_TAB_KEY = "CurrentTabKey"
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteMonitorScreen(initialTab: Int, modifier: Modifier = Modifier) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.site_monitoring),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            },
            content = {
                SiteMonitorHeader(initialTab, modifier = modifier)
            }
        )
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteMonitorHeader(initialTab: Int, modifier: Modifier = Modifier) {
        var tabIndex by remember { mutableIntStateOf(initialTab) }

        val tabs = SiteMonitorTabItem.entries

        LaunchedEffect(true) {
            siteMonitorUtils.trackTabLoaded(tabs[initialTab].siteMonitorType)
        }

        Column(modifier = modifier.fillMaxWidth()) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                indicator = { tabPositions ->
                    // Customizing the indicator color and style
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = MaterialTheme.colors.onSurface,
                        height = 2.0.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, item ->
                    Tab(
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(item.title).uppercase(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        selected = tabIndex == index,
                        onClick = {
                            siteMonitorUtils.trackTabLoaded(tabs[index].siteMonitorType)
                            tabIndex = index
                        },
                    )
                }
            }
            when (tabIndex) {
                0 -> SiteMonitorTabContent(SiteMonitorType.METRICS)
                1 -> SiteMonitorTabContent(SiteMonitorType.PHP_LOGS)
                2 -> SiteMonitorTabContent(SiteMonitorType.WEB_SERVER_LOGS)
            }
        }
    }

    @Composable
    private fun SiteMonitorTabContent(tabType: SiteMonitorType, modifier: Modifier = Modifier) {
        val uiState by remember(key1 = tabType) {
            siteMonitorParentViewModel.getUiState(tabType)
        }
        when (uiState) {
            is SiteMonitorUiState.Preparing -> LoadingState(modifier)
            is SiteMonitorUiState.Prepared, is SiteMonitorUiState.Loaded ->
                SiteMonitorWebViewContent(uiState, tabType, modifier)

            is SiteMonitorUiState.Error -> SiteMonitorError(uiState as SiteMonitorUiState.Error, modifier)
        }
    }

    @Composable
    fun LoadingState(modifier: Modifier = Modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colors.onSurface
            )
        }
    }

    @Composable
    fun SiteMonitorError(error: SiteMonitorUiState.Error, modifier: Modifier = Modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(20.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            androidx.compose.material.Text(
                text = uiStringText(uiString = error.title),
                style = androidx.compose.material.MaterialTheme.typography.h5,
                textAlign = TextAlign.Center
            )
            androidx.compose.material.Text(
                text = uiStringText(uiString = error.description),
                style = androidx.compose.material.MaterialTheme.typography.body1,
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

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun SiteMonitorWebViewContent(
        uiState: SiteMonitorUiState,
        tabType: SiteMonitorType,
        modifier: Modifier = Modifier
    ) {
        // retrieve the webview from the actvity
        val webView = when (tabType) {
            SiteMonitorType.METRICS -> metricsWebView
            SiteMonitorType.PHP_LOGS -> phpLogsWebView
            SiteMonitorType.WEB_SERVER_LOGS -> webServerLogsWebView
        }

        when(uiState) {
            is SiteMonitorUiState.Prepared -> {
                webView.postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, uiState.model.addressToLoad.toByteArray())
                LoadingState()
            }
            is SiteMonitorUiState.Loaded -> {
                SiteMonitorWebView(webView, tabType, modifier)
            }
            else -> {}
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun SiteMonitorWebView(tabWebView: WebView, tabType: SiteMonitorType, modifier: Modifier = Modifier) {
        // the webview is retrieved from the activity, so we need to use a mutable variable
        // to assign to android view
        var webView = tabWebView

        val refreshState = siteMonitorParentViewModel.getRefreshState(tabType)

        val pullRefreshState = rememberPullRefreshState(
            refreshing = refreshState.value,
            onRefresh = { siteMonitorParentViewModel.refreshData(tabType) }
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                item {
                    AndroidView(
                        factory = { webView },
                        update = { webView = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            PullRefreshIndicator(
                refreshing = refreshState.value,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colors.primaryVariant,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        metricsWebView.destroy()
        phpLogsWebView.destroy()
        webServerLogsWebView.destroy()
    }

    override fun onWebViewPageLoaded(url: String, tabType: SiteMonitorType) =
        siteMonitorParentViewModel.onUrlLoaded(tabType)

    override fun onWebViewReceivedError(url: String, tabType: SiteMonitorType) {
        siteMonitorParentViewModel.onWebViewError(tabType)
        siteMonitorUtils.trackTabLoadingError(tabType)
    }
}
