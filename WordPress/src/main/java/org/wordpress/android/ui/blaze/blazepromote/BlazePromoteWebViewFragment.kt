package org.wordpress.android.ui.blaze.blazepromote

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebChromeClientWithFileChooser
import org.wordpress.android.ui.WPWebViewActivity.WPCOM_LOGIN_URL
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeWebViewClient
import org.wordpress.android.ui.blaze.BlazeWebViewHeaderUiState
import org.wordpress.android.ui.blaze.OnBlazeWebViewClientListener
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.compose.state.LoadingState
import org.wordpress.android.editor.R as EditorR

@AndroidEntryPoint
class BlazePromoteWebViewFragment: Fragment(), OnBlazeWebViewClientListener,
    WPWebChromeClientWithFileChooser.OnShowFileChooserListener {
    private var chromeClient: WPWebChromeClientWithFileChooser? = null
    private val blazePromoteWebViewViewModel: BlazePromoteWebViewViewModel by viewModels()
    private val blazeViewModel: BlazeViewModel by activityViewModels()

    companion object {
        fun newInstance() = BlazePromoteWebViewFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM2 {
                BlazeWebViewScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBackPressHandler()
        initViewModel()
        observeViewModel()
    }

    private fun initViewModel() {
        blazePromoteWebViewViewModel.start(blazeViewModel.promoteUiState.value, blazeViewModel.getSource())
    }

    private fun observeViewModel() {
        blazePromoteWebViewViewModel.actionEvents.onEach(this::handleActionEvents)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: BlazeActionEvent) {
        when (actionEvent) {
            is BlazeActionEvent.FinishActivity -> requireActivity().finish()

            is BlazeActionEvent.LaunchExternalBrowser -> {
                ActivityLauncher.openUrlExternal(
                    requireContext(),
                    actionEvent.url
                )
            }
        }
    }

    override fun onWebViewPageLoaded(url: String) {
        blazePromoteWebViewViewModel.updateBlazeFlowStep(url)
        blazePromoteWebViewViewModel.onWebViewPageLoaded()
    }

    override fun onWebViewReceivedError(url: String?) {
        blazePromoteWebViewViewModel.updateBlazeFlowStep(url)
        blazePromoteWebViewViewModel.onWebViewReceivedError()
    }

    override fun onRedirectToExternalBrowser(url: String) {
        blazePromoteWebViewViewModel.onRedirectToExternalBrowser(url)
    }

    @Suppress("DEPRECATION")
    override fun startActivityForFileChooserResult(intent: Intent?, requestCode: Int) {
        intent?.let { startActivityForResult(it, requestCode) }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        chromeClient?.onActivityResult(requestCode, resultCode, data)
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    blazePromoteWebViewViewModel.handleOnBackPressed()
                }
            }
        )
    }

    // The next 2 Composable(s) live in the fragment because they need access to the chromeClient outside of the fun
    // Also the clients needs access to activity and we are not holding on to that elsewhere
    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    private fun BlazeWebViewScreen(
        viewModel: BlazePromoteWebViewViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    ) {
        val uiState by viewModel.uiState.collectAsState()
        val headerState by viewModel.headerUiState.collectAsState()
        Scaffold(
            topBar = { TopAppBar(headerState) },
            content = { BlazePromoteContent(uiState) }
        )
    }

    @Composable
    private fun TopAppBar(
        state: BlazeWebViewHeaderUiState
    ) {
        MainTopAppBar(
            title = stringResource(id = state.headerTitle),
            onNavigationIconClick = {},
            actions = { TopAppBarActions(state = state) }
        )
    }

    @Composable
    private fun TopAppBarActions(state: BlazeWebViewHeaderUiState) {
            TextButton(
                onClick = { blazePromoteWebViewViewModel.onHeaderActionClick(state) },
                enabled = state.headerActionEnabled,
            ) {
                    Text(
                        stringResource(id = state.headerActionText),
                        color = if (state.headerActionEnabled) {
                            MaterialTheme.colors.onSurface
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                        }
                    )
              }
    }

    @Composable
    private fun BlazePromoteContent(uiState: BlazePromoteUiState) {
        when (uiState) {
            is BlazePromoteUiState.Preparing -> LoadingState()
            is BlazePromoteUiState.Loading, is BlazePromoteUiState.Loaded-> BlazeWebViewContent(uiState)
            is BlazePromoteUiState.Error -> BlazePromoteError(uiState)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun BlazeWebViewContent(uiState: BlazePromoteUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }

        if (uiState is BlazePromoteUiState.Loading) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = model.enableJavascript
                    settings.domStorageEnabled = model.enableDomStorage
                    webViewClient = BlazeWebViewClient(this@BlazePromoteWebViewFragment)
                    chromeClient = WPWebChromeClientWithFileChooser(
                        activity,
                        this,
                        EditorR.drawable.media_movieclip,
                        null,
                        this@BlazePromoteWebViewFragment
                    )
                    webChromeClient = chromeClient
                    postUrl(WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is BlazePromoteUiState.Loading) {
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
    fun BlazePromoteError(error: BlazePromoteUiState.Error) {
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
}
