package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import javax.inject.Inject

class PrepareVoiceToContentUseCase @Inject constructor(
    private val jetpackAIStore: JetpackAIStore
) {
    suspend fun execute(site: SiteModel): PrepareVoiceToContentResult =
        withContext(Dispatchers.IO) {
            when (val response = jetpackAIStore.fetchJetpackAIAssistantFeature(site)) {
                is JetpackAIAssistantFeatureResponse.Success -> {
                    PrepareVoiceToContentResult.Success(model = response.model)
                }
                is JetpackAIAssistantFeatureResponse.Error -> {
                    PrepareVoiceToContentResult.Error
                }
            }
        }
}

sealed class PrepareVoiceToContentResult {
    data class Success(val model: JetpackAIAssistantFeature) : PrepareVoiceToContentResult()
    data object Error : PrepareVoiceToContentResult()
}
