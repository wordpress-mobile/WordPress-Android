package org.wordpress.android.ui.jpfullplugininstall.install

import org.wordpress.android.R
import javax.inject.Inject

class JetpackFullPluginInstallUiStateMapper @Inject constructor() {
    fun mapInitial(): UiState.Initial =
        UiState.Initial(
            buttonText = R.string.jetpack_full_plugin_install_initial_button,
        )

    fun mapInstalling(): UiState.Installing = UiState.Installing

    fun mapDone(): UiState.Done =
        UiState.Done(
            buttonText = R.string.jetpack_full_plugin_install_done_button,
        )

    fun mapError(): UiState.Error =
        UiState.Error(
            retryButtonText = R.string.jetpack_full_plugin_install_error_button_retry,
            contactSupportButtonText = R.string.jetpack_full_plugin_install_error_button_contact_support,
        )
}
