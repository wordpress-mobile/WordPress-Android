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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

@SuppressLint("SetJavaScriptEnabled")
@AndroidEntryPoint
class SiteMonitorParentActivity : BaseAppCompatActivity(), SiteMonitorWebViewClient.SiteMonitorWebViewClientListener {
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
            AppThemeM3 {
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SiteMonitorScreen(initialTab: Int) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.site_monitoring)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressedDispatcher::onBackPressed) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back)
                            )
                        }
                    },
                )
            },
        ) { contentPadding ->
            SiteMonitorHeader(initialTab, modifier = Modifier.padding(contentPadding))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteMonitorHeader(initialTab: Int, modifier: Modifier = Modifier) {
        var tabIndex by remember { mutableIntStateOf(initialTab) }
        val tabs = SiteMonitorTabItem.entries

        LaunchedEffect(true) {
            siteMonitorUtils.trackTabLoaded(tabs[initialTab].siteMonitorType)
        }

        Column(modifier = modifier.fillMaxWidth()) {
            PrimaryTabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        color = MaterialTheme.colorScheme.onSurface,
                        height = 2.0.dp,
                        modifier = Modifier.tabIndicatorOffset(tabIndex)
                    )
                }
            ) {
                tabs.forEachIndexed { index, item ->
                    Tab(
                        text = {
                            Text(
                                text = stringResource(item.title).uppercase(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Normal
                            )
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
            is SiteMonitorUiState.Preparing ->
                LoadingState(modifier)

            is SiteMonitorUiState.Prepared, is SiteMonitorUiState.Loaded ->
                SiteMonitorWebViewContent(uiState, tabType, modifier)

            is SiteMonitorUiState.Error ->
                SiteMonitorError(uiState as SiteMonitorUiState.Error, modifier)
        }
    }

    @Composable
    fun LoadingState(modifier: Modifier = Modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurface
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
            Text(
                text = uiStringText(uiString = error.title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiStringText(uiString = error.description),
                style = MaterialTheme.typography.bodyLarge,
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
        val webView = when (tabType) {
            SiteMonitorType.METRICS -> metricsWebView
            SiteMonitorType.PHP_LOGS -> phpLogsWebView
            SiteMonitorType.WEB_SERVER_LOGS -> webServerLogsWebView
        }

        when (uiState) {
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SiteMonitorWebView(
        tabWebView: WebView,
        tabType: SiteMonitorType,
        modifier: Modifier = Modifier
    ) {
        // the webview is retrieved from the activity, so we need to use a mutable variable
        // to assign to android view
        var webView = tabWebView

        val refreshState = siteMonitorParentViewModel.getRefreshState(tabType)
        val pullToRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            modifier = modifier
                .fillMaxSize(),
            isRefreshing = refreshState.value,
            state = pullToRefreshState,
            onRefresh = { siteMonitorParentViewModel.refreshData(tabType) },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = refreshState.value,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
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

    companion object {
        const val ARG_SITE_MONITOR_TYPE_KEY = "ARG_SITE_MONITOR_TYPE_KEY"
        const val SAVED_STATE_CONTAINER_KEY = "ContainerKey"
        const val SAVED_STATE_CURRENT_TAB_KEY = "CurrentTabKey"
    }
}
