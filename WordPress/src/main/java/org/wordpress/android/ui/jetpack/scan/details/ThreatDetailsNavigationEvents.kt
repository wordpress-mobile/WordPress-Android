package org.wordpress.android.ui.jetpack.scan.details

import androidx.annotation.StringRes
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

private const val CODEABLE_GET_FREE_ESTIMATE_FIREBASE_KEY = "codeable_get_free_estimate_url"

sealed class ThreatDetailsNavigationEvents {
    class OpenThreatActionDialog(
        val title: UiString,
        val message: UiString,
        val okButtonAction: () -> Unit
    ) : ThreatDetailsNavigationEvents() {
        @StringRes
        val positiveButtonLabel: Int = R.string.dialog_button_ok
        @StringRes
        val negativeButtonLabel: Int = R.string.dialog_button_cancel
    }

    data class ShowUpdatedScanStateWithMessage(@StringRes val messageRes: Int) : ThreatDetailsNavigationEvents()

    data class ShowUpdatedFixState(val threatId: Long) : ThreatDetailsNavigationEvents()

    object ShowGetFreeEstimate : ThreatDetailsNavigationEvents() {
        fun url() = FirebaseRemoteConfig.getInstance().getString(CODEABLE_GET_FREE_ESTIMATE_FIREBASE_KEY)
    }

    data class ShowJetpackSettings(val url: String) : ThreatDetailsNavigationEvents()
}
