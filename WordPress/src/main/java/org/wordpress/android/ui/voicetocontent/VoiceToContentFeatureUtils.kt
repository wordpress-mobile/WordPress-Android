package org.wordpress.android.ui.voicetocontent

import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.VoiceToContentFeatureConfig
import javax.inject.Inject

class VoiceToContentFeatureUtils @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val voiceToContentFeatureConfig: VoiceToContentFeatureConfig
) {
    fun isVoiceToContentEnabled() = buildConfigWrapper.isJetpackApp && voiceToContentFeatureConfig.isEnabled()

    fun isEligibleForVoiceToContent(jetpackFeatureAIAssistantFeature: JetpackAIAssistantFeature) =
        !jetpackFeatureAIAssistantFeature.siteRequireUpgrade

    fun getRequestLimit(jetpackFeatureAIAssistantFeature: JetpackAIAssistantFeature): Int {
        with (jetpackFeatureAIAssistantFeature) {
            if (currentTier?.slug == JETPACK_AI_FREE) {
                return maxOf(0, requestsLimit - requestsCount)
            }
            // The backend uses `1` as an indicator of unlimited requests.
            if (currentTier?.value == 1) {
                return Int.MAX_VALUE
            }
            // The `usage-period.requests-count` is only valid for paid plans with
            // a limited number of requests.
            val requestsLimit = currentTier?.limit ?: requestsLimit
            val requestsCount = usagePeriod?.requestsCount ?: requestsCount
            return maxOf(0, requestsLimit - requestsCount)
        }
    }

    companion object {
       private const val JETPACK_AI_FREE = "jetpack_ai_free"
    }
}
