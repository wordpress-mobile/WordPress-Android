package org.wordpress.android.ui.jpfullplugininstall.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.ContactSupport
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.InstallJPFullPlugin
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.ActionEvent.OpenTermsAndConditions
import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.state.Loaded
import org.wordpress.android.util.WPUrlUtils

class JetpackFullPluginInstallOnboardingDialogFragment : DialogFragment() {
    private val viewModel: JetpackFullPluginInstallOnboardingViewModel by viewModels()

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
        observeActionEvents()
    }

    @Composable
    private fun JetpackFullPluginInstallOnboardingScreen(
        viewModel: JetpackFullPluginInstallOnboardingViewModel = viewModel()
    ) {
        val uiState by viewModel.uiState.collectAsState()
        uiState.apply {
            when (this) {
                is UiState.Loaded -> Loaded(this)
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
            is InstallJPFullPlugin -> {
                //TODO
            }
            is ContactSupport -> {
                ActivityLauncher.viewHelpAndSupport(
                    requireContext(),
                    actionEvent.origin,
                    actionEvent.selectedSite,
                    null
                )
            }
        }
    }
}
