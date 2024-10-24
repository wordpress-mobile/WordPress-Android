package org.wordpress.android.ui.jetpackplugininstall.remoteplugin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.JetpackConnectionSource
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.jetpackplugininstall.install.UiState
import org.wordpress.android.ui.jetpackplugininstall.install.compose.JetpackPluginInstallScreen
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONNECT
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONTACT_SUPPORT
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.LOGIN
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.MANUAL_INSTALL
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class JetpackRemoteInstallActivity : LocaleAwareActivity() {
    private val viewModel: JetpackRemoteInstallViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM2 {
                val uiState by viewModel.liveViewState.observeAsState()
                JetpackPluginInstallScreen(
                    uiState = uiState ?: UiState.Initial(R.string.jetpack_plugin_install_initial_button),
                    onDismissScreenClick = onBackPressedDispatcher::onBackPressed,
                    onInitialButtonClick = viewModel::onInitialButtonClick,
                    onDoneButtonClick = viewModel::onDoneButtonClick,
                    onRetryButtonClick = viewModel::onRetryButtonClick,
                    onContactSupportButtonClick = viewModel::onContactSupportButtonClick,
                )
            }
        }
        initViewModel(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            if (!viewModel.isBackButtonEnabled()) return@addCallback

            val source = requireNotNull(intent.getSerializableExtraCompat<JetpackConnectionSource>(TRACKING_SOURCE_KEY))
            viewModel.onBackPressed(source)

            onBackPressedDispatcher.onBackPressedCompat(this)
        }
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        val site = requireNotNull(intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE))
        val source = requireNotNull(intent.getSerializableExtraCompat<JetpackConnectionSource>(TRACKING_SOURCE_KEY))
        val retrievedState = savedInstanceState?.getSerializableCompat<JetpackRemoteInstallViewModel.Type>(VIEW_STATE)
        viewModel.initialize(site, retrievedState)

        viewModel.liveActionOnResult.observe(this) { result ->
            if (result != null) {
                when (result.action) {
                    MANUAL_INSTALL -> onManualInstallResultAction(source, result)
                    LOGIN -> onLoginResultAction(source)
                    CONNECT -> onConnectResultAction(source, result)
                    CONTACT_SUPPORT -> onContactSupportResultAction(result)
                }
            }
        }
    }

    private fun onManualInstallResultAction(
        source: JetpackConnectionSource,
        result: JetpackRemoteInstallViewModel.JetpackResultActionData
    ) {
        JetpackConnectionWebViewActivity.startManualFlow(
            this,
            source,
            result.site,
            result.loggedIn
        )
        finish()
    }

    @Suppress("DEPRECATION")
    private fun onLoginResultAction(
        source: JetpackConnectionSource
    ) {
        val loginIntent = Intent(this, LoginActivity::class.java)
        LoginMode.JETPACK_STATS.putInto(loginIntent)
        loginIntent.putExtra(LoginActivity.ARG_JETPACK_CONNECT_SOURCE, source)
        startActivityForResult(loginIntent, RequestCodes.JETPACK_LOGIN)
    }

    private fun onConnectResultAction(
        source: JetpackConnectionSource,
        result: JetpackRemoteInstallViewModel.JetpackResultActionData
    ) {
        JetpackConnectionWebViewActivity.startJetpackConnectionFlow(
            this,
            source,
            result.site,
            result.loggedIn
        )
        finish()
    }

    private fun onContactSupportResultAction(
        result: JetpackRemoteInstallViewModel.JetpackResultActionData
    ) {
        val origin = HelpActivity.Origin.JETPACK_REMOTE_INSTALL_PLUGIN_ERROR
        ActivityLauncher.viewHelp(
            this,
            origin,
            result.site,
            null
        )
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.JETPACK_LOGIN && resultCode == Activity.RESULT_OK) {
            val site = intent!!.getSerializableExtra(WordPress.SITE) as SiteModel
            viewModel.onLogin(site.id)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.liveViewState.value?.let {
            outState.putSerializable(VIEW_STATE, JetpackRemoteInstallViewModel.Type.fromState(it))
        }
    }

    companion object {
        const val TRACKING_SOURCE_KEY = "tracking_source_key"
        private const val VIEW_STATE = "view_state_key"
    }
}
