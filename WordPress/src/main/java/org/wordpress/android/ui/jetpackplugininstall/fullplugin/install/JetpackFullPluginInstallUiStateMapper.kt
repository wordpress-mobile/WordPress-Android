package org.wordpress.android.ui.jetpackplugininstall.fullplugin.install

import org.wordpress.android.R
import org.wordpress.android.ui.jetpackplugininstall.install.UiState
import javax.inject.Inject

class JetpackFullPluginInstallUiStateMapper @Inject constructor() {
    fun mapInitial(): UiState.Initial =
        UiState.Initial(
            buttonText = R.string.jetpack_plugin_install_initial_button,
        )

    fun mapInstalling(): UiState.Installing = UiState.Installing

    fun mapDone(): UiState.Done =
        UiState.Done(
            descriptionText = R.string.jetpack_plugin_install_full_plugin_done_description,
            buttonText = R.string.jetpack_plugin_install_full_plugin_done_button,
        )

    fun mapError(): UiState.Error =
        UiState.Error(
            retryButtonText = R.string.jetpack_plugin_install_error_button_retry,
            contactSupportButtonText = R.string.jetpack_plugin_install_error_button_contact_support,
        )
}
