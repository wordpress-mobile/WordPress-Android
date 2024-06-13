package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.ui.voicetocontent.PrepareVoiceToContentResult.Success
import org.wordpress.android.ui.voicetocontent.PrepareVoiceToContentResult.Failure.NetworkUnavailable
import org.wordpress.android.ui.voicetocontent.PrepareVoiceToContentResult.Failure.RemoteRequestFailure
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class PrepareVoiceToContentUseCase @Inject constructor(
    private val jetpackAIStore: JetpackAIStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val logger: VoiceToContentLogger
) {
    suspend fun execute(site: SiteModel): PrepareVoiceToContentResult =
        withContext(Dispatchers.IO) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                return@withContext NetworkUnavailable
            }
            when (val response = jetpackAIStore.fetchJetpackAIAssistantFeature(site)) {
                is JetpackAIAssistantFeatureResponse.Success -> {
                    Success(model = response.model)
                }
                is JetpackAIAssistantFeatureResponse.Error -> {
                    logger.logError("${response.type.name} - ${response.message}")
                    RemoteRequestFailure
                }
            }
        }
}

sealed class PrepareVoiceToContentResult {
    data class Success(val model: JetpackAIAssistantFeature) : PrepareVoiceToContentResult()
    sealed class Failure: PrepareVoiceToContentResult() {
        data object NetworkUnavailable: Failure()
        data object RemoteRequestFailure: Failure()
    }
}
