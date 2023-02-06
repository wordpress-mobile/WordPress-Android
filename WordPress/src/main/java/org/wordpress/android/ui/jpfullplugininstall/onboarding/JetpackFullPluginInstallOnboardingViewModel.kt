package org.wordpress.android.ui.jpfullplugininstall.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackFullPluginInstallOnboardingViewModel @Inject constructor() : ViewModel() {
    sealed class UiState {
        data class Content(
            val siteName: String,
            val pluginName: String,
            val onTermsAndConditionsClick: () -> Unit,
            val onPrimaryButtonClick: () -> Unit,
            val onSecondaryButtonClick: () -> Unit,
        ) : UiState()
    }
}
