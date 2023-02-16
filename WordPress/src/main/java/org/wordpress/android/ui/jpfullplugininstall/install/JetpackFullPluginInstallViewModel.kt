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
        @StringRes val buttonText: Int,
    ) {
        object Initial : UiState(
            toolbarTitle = R.string.jetpack,
            image = R.drawable.ic_jetpack_logo_green_24dp,
            imageContentDescription = R.string.jetpack_full_plugin_install_image_content_description,
            title = R.string.install_jetpack,
            description = R.string.install_jetpack_message,
            buttonText = R.string.jetpack_full_plugin_install_continue,
        )
    }
}
