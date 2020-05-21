package org.wordpress.android.ui

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
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
            R.string.install_jetpack,
            R.string.install_jetpack_message,
            icon = R.drawable.ic_plans_white_24dp,
            iconTint = R.color.jetpack_green,
            buttonResource = R.string.install_jetpack_continue,
            onClick = onClick
    )

    object Installing : JetpackRemoteInstallViewState(
            INSTALLING,
            R.string.installing_jetpack,
            R.string.installing_jetpack_message,
            icon = R.drawable.ic_plans_white_24dp,
            iconTint = R.color.jetpack_green,
            progressBarVisible = true
    )

    data class Installed(override val onClick: () -> Unit) : JetpackRemoteInstallViewState(
            INSTALLED,
            R.string.jetpack_installed,
            R.string.jetpack_installed_message,
            icon = R.drawable.ic_plans_white_24dp,
            buttonResource = R.string.install_jetpack_continue,
            iconTint = R.color.jetpack_green,
            onClick = onClick
    )

    data class Error(override val onClick: () -> Unit) : JetpackRemoteInstallViewState(
            ERROR,
            R.string.jetpack_installation_problem,
            R.string.jetpack_installation_problem_message,
            icon = R.drawable.img_illustration_info_outline_88dp,
            buttonResource = R.string.install_jetpack_retry,
            onClick = onClick
    )

    enum class Type {
        START, INSTALLING, INSTALLED, ERROR
    }
}
