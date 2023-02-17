package org.wordpress.android.ui.jpfullplugininstall.install

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R

class JetpackFullPluginInstallViewModel {
    sealed class UiState(
        @StringRes val toolbarTitle: Int,
        @DrawableRes val image: Int,
        @StringRes val imageContentDescription: Int,
        @StringRes val title: Int,
        @StringRes val description: Int,
    ) {
        data class Initial(
            @StringRes val buttonText: Int = R.string.jetpack_full_plugin_install_initial_button,
        ) : UiState(
            toolbarTitle = R.string.jetpack,
            image = R.drawable.ic_jetpack_logo_green_24dp,
            imageContentDescription = R.string.jetpack_full_plugin_install_jp_logo_content_description,
            title = R.string.jetpack_full_plugin_install_initial_title,
            description = R.string.jetpack_full_plugin_install_initial_description,
        )

        object Installing : UiState(
            toolbarTitle = R.string.jetpack,
            image = R.drawable.ic_jetpack_logo_green_24dp,
            imageContentDescription = R.string.jetpack_full_plugin_install_jp_logo_content_description,
            title = R.string.jetpack_full_plugin_install_installing_title,
            description = R.string.jetpack_full_plugin_install_installing_description,
        )

        data class Done(
            @StringRes val buttonText: Int = R.string.jetpack_full_plugin_install_done_button,
        ) : UiState(
            toolbarTitle = R.string.jetpack,
            image = R.drawable.ic_jetpack_logo_green_24dp,
            imageContentDescription = R.string.jetpack_full_plugin_install_jp_logo_content_description,
            title = R.string.jetpack_full_plugin_install_done_title,
            description = R.string.jetpack_full_plugin_install_done_description,
        )

        data class Error(
            @StringRes val retryButtonText: Int = R.string.jetpack_full_plugin_install_error_button_retry,
            @StringRes val contactSupportButtonText: Int =
                R.string.jetpack_full_plugin_install_error_button_contact_support,
        ) : UiState(
            toolbarTitle = R.string.jetpack,
            image = R.drawable.img_illustration_info_outline_88dp,
            imageContentDescription = R.string.jetpack_full_plugin_install_error_image_content_description,
            title = R.string.jetpack_full_plugin_install_error_title,
            description = R.string.jetpack_full_plugin_install_error_description,
        )
    }
}
