package org.wordpress.android.ui.voicetocontent

import org.wordpress.android.fluxc.model.SiteModel

sealed class VoiceToContentActionEvent {
    data class LaunchEditPost(val site: SiteModel, val content: String) : VoiceToContentActionEvent()
}
