package org.wordpress.android.ui.sitemonitor

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.utils.uiStringText

@AndroidEntryPoint
class SiteMonitorTabFragment : Fragment(), SiteMonitorWebViewClient.SiteMonitorWebViewClientListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(javaClass.simpleName, "***=> onCreate")
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            SiteMonitorTabContent()
        }
    }

    private val viewModel: SiteMonitorTabViewModel by viewModels()
    private val parentViewModel: SiteMonitorParentViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(javaClass.simpleName, "***=> onViewCreated")
        initViewModel(getSiteMonitorType(), getUrlTemplate(), getSite())
    }

    @Suppress("DEPRECATION")
    private fun getSite(): SiteModel {
        return requireNotNull(arguments?.getSerializable(WordPress.SITE)) as SiteModel
    }

    private fun getUrlTemplate(): String {
        return requireNotNull(arguments?.getString(KEY_URL_TEMPLATE))
    }

    @Suppress("DEPRECATION")
    private fun getSiteMonitorType(): SiteMonitorType {
        return requireNotNull(arguments?.getSerializable(KEY_SITE_MONITOR_TYPE)) as SiteMonitorType
    }

    private fun initViewModel(type: SiteMonitorType, urlTemplate: String, site: SiteModel) {
        Log.i(javaClass.simpleName, "***=> initViewModel")
        viewModel.start(type, urlTemplate, site, parentViewModel.getWebViewState(type)!=null)
        viewModel.webViewState.observe(viewLifecycleOwner) { webViewState ->
            webViewState?.let {
                parentViewModel.saveWebViewState(getSiteMonitorType(), it)
            }
        }
    }

    // Custom functions for saving and restoring WebView state
    private fun saveWebViewState(webView: WebView): SiteMonitorWebViewState {
        val bundle = Bundle()
        webView.saveState(bundle)
        return SiteMonitorWebViewState(bundle)
    }

    private fun restoreWebViewState(webView: WebView, state: SiteMonitorWebViewState) {
        try {
            webView.restoreState(state.bundle)
        } catch (e: Exception) {
            // todo: annmarie - track this somewhere
            Log.e(javaClass.simpleName, "***=> restoreWebViewState", e)
        }
    }

    override fun onWebViewPageLoaded(url: String)  = viewModel.onUrlLoaded()

    override fun onWebViewReceivedError(url: String) = viewModel.onWebViewError()

    companion object {
        const val KEY_URL_TEMPLATE = "KEY_URL"
        const val KEY_SITE_MONITOR_TYPE = "KEY_SITE_MONITOR_TYPE"
        fun newInstance(url: String, type: SiteMonitorType, site: SiteModel): Fragment {
            val fragment = SiteMonitorTabFragment()
            val argument = Bundle()
            argument.putString(KEY_URL_TEMPLATE, url)
            argument.putSerializable(KEY_SITE_MONITOR_TYPE, type)
            argument.putSerializable(WordPress.SITE, site)
            fragment.arguments = argument
            return fragment
        }
    }

    @Composable
    private fun SiteMonitorTabContent() {
        val uiState by viewModel.uiState.collectAsState()
        when (uiState) {
            is SiteMonitorUiState.Preparing -> LoadingState()
            is SiteMonitorUiState.Prepared, is SiteMonitorUiState.Loaded -> SiteMonitorWebView(uiState)
            is SiteMonitorUiState.Error -> SiteMonitorError(uiState as SiteMonitorUiState.Error)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun SiteMonitorWebView(uiState: SiteMonitorUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }

        if (uiState is SiteMonitorUiState.Prepared) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = SiteMonitorWebViewClient(this@SiteMonitorTabFragment)
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
                    // todo: annmarie - you may need to move the save here
                    AndroidView(
                        factory = { theWebView },
                        modifier = Modifier.fillMaxSize(),
                        update = { saveWebViewState(theWebView).let { newState ->
                            Log.i(javaClass.simpleName,"***=> SAVE webview state ")
                            viewModel.saveWebViewState(newState)
                            }
                        }
                    )
                } ?: run {
                    Log.i(javaClass.simpleName, "***=> Load -> The webview is null")
                    val currentState = parentViewModel.getWebViewState(getSiteMonitorType())
                    currentState?.let { state ->
                        Log.i(javaClass.simpleName, "***=> RESTORE webview state ")
                        webView = WebView(requireContext()).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                            settings.userAgentString = (uiState as SiteMonitorUiState.Loaded).userAgent
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                           // webViewClient = SiteMonitorWebViewClient(this@SiteMonitorTabFragment)
                            // todo: annmarie - is this wrong - no this is correct
                            restoreWebViewState(this, state)
                        }

                        Log.i(javaClass.simpleName, "***=> HOLY FUDGE IS THE webVIEW NULL AGAIN ${webView == null}")
                        webView?.let { theWebView ->
                            Log.i(javaClass.simpleName, "***=> Show the webview - restored")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AndroidView(
                                    factory = { theWebView },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

//    @SuppressLint("SetJavaScriptEnabled")
//    @Composable
//    private fun SiteMonitorWebView(uiState: SiteMonitorUiState) {
//        var webView: WebView? by remember { mutableStateOf(null) }
//
//        LaunchedEffect(true) {
//            if (uiState is SiteMonitorUiState.Prepared && webView == null) {
//                val model = uiState.model
//                webView = WebView(requireContext()).apply {
//                    layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
//                    settings.userAgentString = model.userAgent
//                    settings.javaScriptEnabled = true
//                    settings.domStorageEnabled = true
//                    webViewClient = SiteMonitorWebViewClient(this@SiteMonitorTabFragment)
//                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
//                }
//            }
//        }
//
//        DisposableEffect(webView) {
//            onDispose {
//                // Save the WebView state when it is disposed
//                webView?.let { theWebView ->
//                    saveWebViewState(theWebView).let { newState ->
//                        Log.i(javaClass.simpleName,"***=> SAVE webview state ")
//                        viewModel.saveWebViewState(newState)
//                    }
//                }
//            }
//        }
//
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            if (uiState is SiteMonitorUiState.Prepared) {
//                LoadingState()
//            } else {
//                webView?.let { theWebView ->
//                    AndroidView(
//                        factory = { theWebView },
//                        modifier = Modifier.fillMaxSize(),
//                        update = {
//                            // todo: annmarie - you may need to move the save here
//                            saveWebViewState(theWebView).let { newState ->
//                                Log.i(javaClass.simpleName,"***=> SAVE webview state ")
//                                viewModel.saveWebViewState(newState)
//                            }
//                        }
//                    )
//                } ?: run {
//                    Log.i(javaClass.simpleName, "***=> Load -> The webview is null")
//                    val currentState = parentViewModel.getWebViewState(getSiteMonitorType())
//                    currentState?.let { state ->
//                        Log.i(javaClass.simpleName, "***=> RESTORE webview state ")
//                        webView = WebView(requireContext()).apply {
//                            layoutParams = ViewGroup.LayoutParams(
//                                ViewGroup.LayoutParams.MATCH_PARENT,
//                                ViewGroup.LayoutParams.MATCH_PARENT
//                            )
//                            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
//                            settings.userAgentString = (uiState as SiteMonitorUiState.Loaded).userAgent
//                            settings.javaScriptEnabled = true
//                            settings.domStorageEnabled = true
//                            webViewClient = SiteMonitorWebViewClient(this@SiteMonitorTabFragment)
//                            // todo: annmarie - is this wrong?
//                            restoreWebViewState(this, state)
//                        }
//
//                        Log.i(javaClass.simpleName, "***=> HOLY FUDGE IS THE webVIEW NULL AGAIN ${webView == null}")
//                        webView?.let { theWebView ->
//                            Log.i(javaClass.simpleName, "***=> Show the webview - restored")
//                            Box(
//                                modifier = Modifier.fillMaxSize(),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                AndroidView(
//                                    factory = { theWebView },
//                                    modifier = Modifier.fillMaxSize(),
//                                    update = {
//                                        // todo: annmarie - you may need to move the save here
//                                        saveWebViewState(theWebView).let { newState ->
//                                            Log.i(javaClass.simpleName,"***=> SAVE webview state ")
//                                            viewModel.saveWebViewState(newState)
//                                        }
//                                    }
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }


//    @SuppressLint("SetJavaScriptEnabled")
//    @Composable
//    private fun SiteMonitorWebView(uiState: SiteMonitorUiState) {
//        var webView: WebView? by remember { mutableStateOf(null) }
//
//        DisposableEffect(webView) {
//            onDispose {
//                // Save the WebView state when it's disposed
//                webView?.let { theWebView ->
//                    saveWebViewState(theWebView).let { newState ->
//                        Log.i(javaClass.simpleName, "***=> SAVE webview state ")
//                        viewModel.saveWebViewState(newState)
//                    }
//                }
//            }
//        }
//
//        Box(
//            modifier = Modifier.fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            when (uiState) {
//                is SiteMonitorUiState.Prepared -> {
//                    AndroidView(
//                        factory = { context ->
//                            WebView(context).apply {
//                                layoutParams = ViewGroup.LayoutParams(
//                                    ViewGroup.LayoutParams.MATCH_PARENT,
//                                    ViewGroup.LayoutParams.MATCH_PARENT
//                                )
//                                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
//                                settings.userAgentString = uiState.model.userAgent
//                                settings.javaScriptEnabled = true
//                                settings.domStorageEnabled = true
//                                webViewClient = SiteMonitorWebViewClient(this@SiteMonitorTabFragment)
//
//                                // Restore WebView state if available
//                                val currentState = parentViewModel.getWebViewState(getSiteMonitorType())
//                                currentState?.let { state ->
//                                    Log.i(javaClass.simpleName, "***=> RESTORE webview state ")
//                                    restoreWebViewState(this, state)
//                                }
//
//                                // Load URL if the WebView has not been loaded before
//                                if (webView == null) {
//                                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, uiState.model.addressToLoad.toByteArray())
//                                }
//
//                                // Update the reference to the current WebView instance
//                                webView = this
//                            }
//                        },
//                        modifier = Modifier.fillMaxSize()
//                    )
//                }
//                is SiteMonitorUiState.Loading -> {
//                    // Show loading state
//                    LoadingState()
//                }
//                else -> {
//                    // Handle other states as needed
//                }
//            }
//        }
//    }


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
                    androidx.compose.material.Text(text = uiStringText(uiString = error.button.text))
                }
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}
