package org.wordpress.android.ui.jpfullplugininstall.onboarding

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject

@HiltViewModel
class JetpackFullPluginInstallOnboardingViewModel @Inject constructor() : ViewModel() {

    sealed class UiState {
        data class Content(
            val title: UiString,
            val subtitle: UiString,
            val message: UiString,
            val primaryActionButtonText: UiString,
            val primaryActionButtonClick: () -> Unit,
            val secondaryActionButtonText: UiString,
            val secondaryActionButtonClick: () -> Unit,
        ) : UiState()
    }
}
