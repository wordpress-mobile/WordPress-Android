package org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.install.JetpackFullPluginInstallActivity
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.ContactSupport
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.Dismiss
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.OpenInstallJetpackFullPlugin
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.OpenTermsAndConditions
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.compose.state.LoadedState
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.onBackPressedCompat
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor

@AndroidEntryPoint
class JetpackFullPluginInstallOnboardingDialogFragment : DialogFragment() {
    private val viewModel: JetpackFullPluginInstallOnboardingViewModel by viewModels()

    override fun getTheme(): Int {
        return R.style.JetpackFullPluginInstallOnboardingDialogFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                JetpackFullPluginInstallOnboardingScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onScreenShown()
        observeActionEvents()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            (this as ComponentDialog).onBackPressedDispatcher
                .addCallback(this@JetpackFullPluginInstallOnboardingDialogFragment) {
                    viewModel.onDismissScreenClick()
                    onBackPressedDispatcher.onBackPressedCompat(this)
                }
            setStatusBarAsSurfaceColor()
        }

    @Composable
    private fun JetpackFullPluginInstallOnboardingScreen(
        viewModel: JetpackFullPluginInstallOnboardingViewModel = viewModel()
    ) {
        val uiState by viewModel.uiState.collectAsState()
        uiState.apply {
            when (this) {
                is UiState.Loaded -> LoadedState(
                    content = this,
                    onTermsAndConditionsClick = { viewModel.onTermsAndConditionsClick() },
                    onInstallFullPluginClick = { viewModel.onInstallFullPluginClick() },
                    onContactSupportClick = { viewModel.onContactSupportClick() },
                    onDismissScreenClick = { viewModel.onDismissScreenClick() }
                )
                is UiState.None -> {}
            }
        }
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: ActionEvent) {
        when (actionEvent) {
            is OpenTermsAndConditions -> {
                WPWebViewActivity.openURL(
                    requireContext(),
                    WPUrlUtils.buildTermsOfServiceUrl(requireContext())
                )
            }
            is OpenInstallJetpackFullPlugin -> {
                startActivity(JetpackFullPluginInstallActivity.createIntent(requireContext()))
                dismiss()
            }
            is ContactSupport -> {
                ActivityLauncher.viewHelp(
                    requireContext(),
                    actionEvent.origin,
                    actionEvent.selectedSite,
                    null
                )
            }
            is Dismiss -> {
                dismiss()
            }
        }.exhaustive
    }

    companion object {
        const val TAG = "JETPACK_FULL_PLUGIN_INSTALL_ONBOARDING_DIALOG_FRAGMENT"

        @JvmStatic
        fun newInstance(): JetpackFullPluginInstallOnboardingDialogFragment =
            JetpackFullPluginInstallOnboardingDialogFragment()
    }
}
