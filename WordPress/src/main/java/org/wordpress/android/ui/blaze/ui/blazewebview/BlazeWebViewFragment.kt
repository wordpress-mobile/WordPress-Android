package org.wordpress.android.ui.blaze.ui.blazewebview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.WPWebChromeClientWithFileChooser
import org.wordpress.android.ui.WPWebViewActivity.WPCOM_LOGIN_URL
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.blaze.BlazeWebViewHeaderUiState
import org.wordpress.android.ui.blaze.BlazeWebViewClient
import org.wordpress.android.ui.blaze.BlazeWebViewContentUiState
import org.wordpress.android.ui.blaze.OnBlazeWebViewClientListener
import org.wordpress.android.ui.blaze.ui.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.compose.theme.AppTheme

@Suppress("ForbiddenComment")
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
        }
    }

    // The next 2 Composable(s) live in the fragment because they need access to the chromeClient outside of the fun
    // Also the clients needs access to activity and we are not holding on to that elsewhere
    // (1) Should the title be centered? The mocks only show iOS and not Android???
    // (2) withFullContentAlpha wrapped around the text, but this is hard to do if wrapped in another composable?
    @Composable
    private fun BlazeWebViewScreen(
        viewModel: BlazeWebViewViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    ) {
        val data by viewModel.model.collectAsState()
        val blazeHeaderState by viewModel.blazeHeaderState.collectAsState()
        Scaffold(
            // todo: Move onClick into state possibly
            topBar = { TopAppBar(blazeHeaderState, viewModel::onHeaderActionClick) },
            content = { BlazeWebViewContent(data) }
        )
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
        // todo Track events for started/completed
        // blaze_flow_started	Webview: Blaze flow started	Entry point source
        // blaze_flow_completed	Webview: Blaze flow completed	Entry point source (when possible)
        blazeWebViewViewModel.hideOrShowCancelAction(url)
    }

    override fun onWebViewReceivedError() {
        blazeWebViewViewModel.onWebViewReceivedError()
    }

    @Suppress("DEPRECATION")
    override fun startActivityForFileChooserResult(intent: Intent?, requestCode: Int) {
        startActivityForResult(intent, requestCode)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        chromeClient?.onActivityResult(requestCode, resultCode, data)
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // no op
                }
            }
        )
    }

    companion object {
        fun newInstance() = BlazeWebViewFragment()

        // todo - add the arguments needed OR potentially get them from the parentViewModel
        const val POST_ID = "post_id"
    }
}

@Composable
private fun TopAppBar(
    state: BlazeWebViewHeaderUiState,
    onHeaderActionClick: () -> Unit
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 0.dp,
        title = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                //withFullContentAlpha { I don't think I can use this here
                Text(
                    stringResource(id = state.headerTitle),
                    color = MaterialTheme.colors.onSurface
                )
                // }
            }
        },
        actions = {
            if (state.headerActionVisible) {
                TextButton(
                    onClick = { onHeaderActionClick() }
                ) {
                    Text(
                        stringResource(id = state.headerActionText),
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
    ) // TopAppBar
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAppBarPreview() {
    val  onClick : () -> Unit = {}
    TopAppBar(BlazeWebViewHeaderUiState.ShowAction(), onClick )
}


