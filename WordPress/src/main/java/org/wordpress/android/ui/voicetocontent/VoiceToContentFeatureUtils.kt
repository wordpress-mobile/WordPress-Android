package org.wordpress.android.ui.voicetocontent

import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.VoiceToContentFeatureConfig
import javax.inject.Inject

class VoiceToContentFeatureUtils @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val voiceToContentFeatureConfig: VoiceToContentFeatureConfig
) {
    // todo: remove buildConfigWrapper.isDebug() when Voice to content is ready for release
    fun isVoiceToContentEnabled() = buildConfigWrapper.isJetpackApp
            && voiceToContentFeatureConfig.isEnabled()
            && buildConfigWrapper.isDebug()

    fun isEligibleForVoiceToContent(jetpackFeatureAIAssistantFeature: JetpackAIAssistantFeature) =
        !jetpackFeatureAIAssistantFeature.siteRequireUpgrade

    fun getRequestLimit(jetpackFeatureAIAssistantFeature: JetpackAIAssistantFeature): Int {
        return with(jetpackFeatureAIAssistantFeature) {
            val calculatedLimit = if (currentTier?.slug == JETPACK_AI_FREE) {
                maxOf(0, requestsLimit - requestsCount)
            } else if (currentTier?.value == 1) {
                Int.MAX_VALUE
            } else {
                val requestsLimit = currentTier?.limit ?: requestsLimit
                val requestsCount = usagePeriod?.requestsCount ?: requestsCount
                maxOf(0, requestsLimit - requestsCount)
            }
            calculatedLimit
        }
    }

    companion object {
       private const val JETPACK_AI_FREE = "jetpack_ai_free"
    }
}
