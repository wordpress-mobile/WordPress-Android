package org.wordpress.android.ui.voicetocontent

import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.VoiceToContentFeatureConfig
import javax.inject.Inject

class VoiceToContentFeatureUtils @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val voiceToContentFeatureConfig: VoiceToContentFeatureConfig
) {
    fun isVoiceToContentEnabled() = buildConfigWrapper.isJetpackApp && voiceToContentFeatureConfig.isEnabled()
}
