package org.wordpress.android.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.ui.JetpackRemoteInstallComposeViewModel.JetpackResultActionData.Action.CONNECT
import org.wordpress.android.ui.JetpackRemoteInstallComposeViewModel.JetpackResultActionData.Action.LOGIN
import org.wordpress.android.ui.JetpackRemoteInstallComposeViewModel.JetpackResultActionData.Action.MANUAL_INSTALL
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jpfullplugininstall.install.JetpackFullPluginInstallScreen
import org.wordpress.android.ui.jpfullplugininstall.install.UiState

@AndroidEntryPoint
class JetpackRemoteInstallComposeFragment : Fragment() {
    private val viewModel: JetpackRemoteInstallComposeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val uiState by viewModel.liveViewState.observeAsState()
                JetpackFullPluginInstallScreen(
                    uiState = uiState ?: UiState.Initial(R.string.install_jetpack_continue),
                    onDismissScreenClick = {},
                    onContinueClick = viewModel::start,
                    onDoneClick = viewModel::connect,
                    onRetryClick = viewModel::restart,
                    onContactSupportClick = {},
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel(savedInstanceState)
    }


    private fun initViewModel(savedInstanceState: Bundle?) {
        requireActivity().let { activity ->
            val intent = activity.intent
            val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            val source = intent.getSerializableExtra(TRACKING_SOURCE_KEY) as JetpackConnectionSource
            val retrievedState = savedInstanceState
                ?.getSerializable(VIEW_STATE) as? JetpackRemoteInstallComposeViewModel.Type
            viewModel.initialize(site, retrievedState)

            viewModel.liveActionOnResult.observe(viewLifecycleOwner) { result ->
                if (result != null) {
                    when (result.action) {
                        MANUAL_INSTALL -> onManualInstallResultAction(activity, source, result)
                        LOGIN -> onLoginResultAction(activity, source)
                        CONNECT -> onConnectResultAction(activity, source, result)
                    }
                }
            }
        }
    }

    private fun onManualInstallResultAction(
        activity: FragmentActivity,
        source: JetpackConnectionSource,
        result: JetpackRemoteInstallComposeViewModel.JetpackResultActionData
    ) {
        JetpackConnectionWebViewActivity.startManualFlow(
            activity,
            source,
            result.site,
            result.loggedIn
        )
        activity.finish()
    }

    @Suppress("DEPRECATION")
    private fun onLoginResultAction(
        activity: FragmentActivity,
        source: JetpackConnectionSource
    ) {
        val loginIntent = Intent(activity, LoginActivity::class.java)
        LoginMode.JETPACK_STATS.putInto(loginIntent)
        loginIntent.putExtra(LoginActivity.ARG_JETPACK_CONNECT_SOURCE, source)
        startActivityForResult(loginIntent, RequestCodes.JETPACK_LOGIN)
    }

    private fun onConnectResultAction(
        activity: FragmentActivity,
        source: JetpackConnectionSource,
        result: JetpackRemoteInstallComposeViewModel.JetpackResultActionData
    ) {
        JetpackConnectionWebViewActivity.startJetpackConnectionFlow(
            activity,
            source,
            result.site,
            result.loggedIn
        )
        activity.finish()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.JETPACK_LOGIN && resultCode == Activity.RESULT_OK) {
            val site = requireActivity().intent!!.getSerializableExtra(WordPress.SITE) as SiteModel
            viewModel.onLogin(site.id)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.liveViewState.value?.let {
            outState.putSerializable(VIEW_STATE, JetpackRemoteInstallComposeViewModel.Type.fromState(it))
        }
    }

    companion object {
        const val TRACKING_SOURCE_KEY = "tracking_source_key"
        private const val VIEW_STATE = "view_state_key"
    }
}
