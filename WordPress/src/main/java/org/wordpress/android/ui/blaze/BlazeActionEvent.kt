package org.wordpress.android.ui.blaze

sealed class BlazeActionEvent {
    object FinishActivity : BlazeActionEvent()
    data class LaunchExternalBrowser(val url: String) : BlazeActionEvent()
}
