package org.wordpress.android.ui.voicetocontent

import org.wordpress.android.fluxc.model.SiteModel

sealed class VoiceToContentActionEvent {
    data object Dismiss: VoiceToContentActionEvent()
    data class LaunchEditPost(val site: SiteModel, val content: String) : VoiceToContentActionEvent()
    data class LaunchExternalBrowser(val url: String) : VoiceToContentActionEvent()
    data object RequestPermission : VoiceToContentActionEvent()
}
