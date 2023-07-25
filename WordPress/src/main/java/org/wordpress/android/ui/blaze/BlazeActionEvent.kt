package org.wordpress.android.ui.blaze

import androidx.annotation.StringRes

sealed class BlazeActionEvent {
    object FinishActivity : BlazeActionEvent()
    data class LaunchExternalBrowser(val url: String) : BlazeActionEvent()
    data class FinishActivityWithMessage(@StringRes val id: Int) : BlazeActionEvent()
}
