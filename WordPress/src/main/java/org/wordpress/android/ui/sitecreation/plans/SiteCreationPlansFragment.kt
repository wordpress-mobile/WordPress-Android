package org.wordpress.android.ui.sitecreation.plans

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.compose.state.LoadingState
import org.wordpress.android.ui.sitecreation.SiteCreationActivity.Companion.ARG_STATE
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.plans.SiteCreationPlansWebViewClient.SiteCreationPlansWebViewClientListener
import org.wordpress.android.util.extensions.getParcelableCompat

@AndroidEntryPoint
class SiteCreationPlansFragment : Fragment(), SiteCreationPlansWebViewClientListener {
    private val viewModel: SiteCreationPlansViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM2 {
                SiteCreationPlansPage(
                    navigationUp = requireActivity().onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start(requireNotNull(requireArguments().getParcelableCompat(ARG_STATE)))
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: SiteCreationPlansActionEvent) {
        when (actionEvent) {
            is SiteCreationPlansActionEvent.CreateSite -> {
                (requireActivity() as PlansScreenListener).onPlanSelected(actionEvent.planModel, actionEvent.domainName)
            }
        }
    }

    // SiteCreationWebViewClient
    override fun onPlanSelected(uri: Uri) {
        viewModel.onPlanSelected(uri)
    }

    override fun onWebViewPageLoaded() {
        viewModel.onUrlLoaded()
    }

    override fun onWebViewReceivedError() {
        viewModel.onWebViewError()
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun SiteCreationPlansPage(
        navigationUp: () -> Unit = { },
        viewModel: SiteCreationPlansViewModel = viewModel(),
    ) {
        val uiState by viewModel.uiState.collectAsState()
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(R.string.site_creation_plans_selection_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = navigationUp
                )
            },
            content = { SiteCreationPlansContent(uiState) }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun SiteCreationPlansContent(uiState: SiteCreationPlansUiState) {
        when (uiState) {
            is SiteCreationPlansUiState.Preparing -> LoadingState()
            is SiteCreationPlansUiState.Prepared,
            is SiteCreationPlansUiState.Loaded -> SiteCreationPlansWebView(uiState)
            is SiteCreationPlansUiState.Error -> SiteCreationPlansError(uiState)
        }
    }

    @Composable
    fun SiteCreationPlansError(error: SiteCreationPlansUiState.Error) {
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

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun SiteCreationPlansWebView(uiState: SiteCreationPlansUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }
        val viewModel: SiteCreationPlansViewModel = viewModel()

        if (uiState is SiteCreationPlansUiState.Prepared) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(requireContext()).apply {
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let { message ->
                                val calypsoErrorMessage = "Uncaught TypeError: window.AppBoot is not a function"
                                if (message.message().contains(calypsoErrorMessage)) {
                                    viewModel.onCalypsoError()
                                }
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = model.enableJavascript
                    settings.domStorageEnabled = model.enableDomStorage
                    webViewClient = SiteCreationPlansWebViewClient(this@SiteCreationPlansFragment)
                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is SiteCreationPlansUiState.Prepared) {
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

    companion object {
        const val TAG = "site_creation_plans_fragment_tag"

        fun newInstance(siteCreationState: SiteCreationState) = SiteCreationPlansFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_STATE, siteCreationState)
            }
        }
    }
}
