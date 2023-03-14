package org.wordpress.android.ui.blaze.ui.blazewebview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebChromeClientWithFileChooser
import org.wordpress.android.ui.WPWebViewActivity.WPCOM_LOGIN_URL
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeWebViewHeaderUiState
import org.wordpress.android.ui.blaze.BlazeWebViewClient
import org.wordpress.android.ui.blaze.BlazeWebViewContentUiState
import org.wordpress.android.ui.blaze.OnBlazeWebViewClientListener
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.theme.AppTheme

@AndroidEntryPoint
class BlazeWebViewFragment: Fragment(), OnBlazeWebViewClientListener,
    WPWebChromeClientWithFileChooser.OnShowFileChooserListener {
    private var chromeClient: WPWebChromeClientWithFileChooser? = null
    private val blazeWebViewViewModel: BlazeWebViewViewModel by viewModels()
    private val blazeViewModel: BlazeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
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
        blazeWebViewViewModel.start(blazeViewModel.promoteUiState.value, blazeViewModel.getSource())
    }

    private fun observeViewModel() {
        blazeWebViewViewModel.actionEvents.onEach(this::handleActionEvents).launchIn(viewLifecycleOwner.lifecycleScope)
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

    // The next 2 Composable(s) live in the fragment because they need access to the chromeClient outside of the fun
    // Also the clients needs access to activity and we are not holding on to that elsewhere
    @Composable
    private fun BlazeWebViewScreen(
        viewModel: BlazeWebViewViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    ) {
        val data by viewModel.model.collectAsState()
        val blazeHeaderState by viewModel.blazeHeaderState.collectAsState()
        Scaffold(
            topBar = { TopAppBar(blazeHeaderState) },
            content = { BlazeWebViewContent(data) }
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
                onClick = { blazeWebViewViewModel.onHeaderActionClick(state) },
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

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun BlazeWebViewContent(model: BlazeWebViewContentUiState) {
        AndroidView(factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                settings.userAgentString = model.userAgent
                settings.javaScriptEnabled = model.enableJavascript
                settings.domStorageEnabled = model.enableDomStorage
                webViewClient = BlazeWebViewClient(this@BlazeWebViewFragment)
                chromeClient = WPWebChromeClientWithFileChooser(
                    activity,
                    this,
                    R.drawable.media_movieclip,
                    null,
                    this@BlazeWebViewFragment
                )
                webChromeClient = chromeClient
                postUrl(WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
            }
        })
    }

    override fun onWebViewPageLoaded(url: String) {
        blazeWebViewViewModel.updateBlazeFlowStep(url)
        blazeWebViewViewModel.updateHeaderActionUiState()
    }

    override fun onWebViewReceivedError(url: String?) {
        blazeWebViewViewModel.updateBlazeFlowStep(url)
        blazeWebViewViewModel.onWebViewReceivedError()
    }

    override fun onRedirectToExternalBrowser(url: String) {
        blazeWebViewViewModel.onRedirectToExternalBrowser(url)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun startActivityForFileChooserResult(intent: Intent?, requestCode: Int) {
        startActivityForResult(intent, requestCode)
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
                    blazeWebViewViewModel.handleOnBackPressed()
                }
            }
        )
    }

    companion object {
        fun newInstance() = BlazeWebViewFragment()
    }
}
