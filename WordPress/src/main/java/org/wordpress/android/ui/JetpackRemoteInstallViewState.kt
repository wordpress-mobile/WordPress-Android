package org.wordpress.android.ui

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.ERROR
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.INSTALLED
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.INSTALLING
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.START

sealed class JetpackRemoteInstallViewState(
    val type: Type,
    @StringRes val titleResource: Int,
    @StringRes val messageResource: Int,
    @DrawableRes val icon: Int,
    @ColorRes val iconTint: Int? = null,
    @StringRes val buttonResource: Int? = null,
    open val onClick: () -> Unit = {},
    val progressBarVisible: Boolean = false
) {
    data class Start(override val onClick: () -> Unit) : JetpackRemoteInstallViewState(
            START,
            string.install_jetpack,
            string.install_jetpack_message,
            icon = drawable.ic_plans_white_24dp,
            iconTint = color.jetpack,
            buttonResource = string.install_jetpack_continue,
            onClick = onClick
    )

    object Installing : JetpackRemoteInstallViewState(
            INSTALLING,
            string.installing_jetpack,
            string.installing_jetpack_message,
            icon = drawable.ic_plans_white_24dp,
            iconTint = color.jetpack,
            progressBarVisible = true
    )

    data class Installed(override val onClick: () -> Unit) : JetpackRemoteInstallViewState(
            INSTALLED,
            string.jetpack_installed,
            string.jetpack_installed_message,
            icon = drawable.ic_plans_white_24dp,
            buttonResource = string.install_jetpack_continue,
            iconTint = color.jetpack,
            onClick = onClick
    )

    data class Error(override val onClick: () -> Unit) : JetpackRemoteInstallViewState(
            ERROR,
            string.jetpack_installation_problem,
            string.jetpack_installation_problem_message,
            icon = drawable.img_illustration_info_outline_88dp,
            buttonResource = string.install_jetpack_retry,
            onClick = onClick
    )

    enum class Type {
        START, INSTALLING, INSTALLED, ERROR
    }
}
