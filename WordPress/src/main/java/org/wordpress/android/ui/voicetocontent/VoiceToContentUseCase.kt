package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIQueryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.ui.voicetocontent.VoiceToContentResult.Failure.NetworkUnavailable
import org.wordpress.android.ui.voicetocontent.VoiceToContentResult.Failure.RemoteRequestFailure
import org.wordpress.android.ui.voicetocontent.VoiceToContentResult.Success
import java.io.File
import javax.inject.Inject

class VoiceToContentUseCase @Inject constructor(
    private val jetpackAIStore: JetpackAIStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val logger: VoiceToContentTelemetry
) {
    companion object {
        const val FEATURE = "voice_to_content"
        const val ROLE = "jetpack-ai"
        const val TYPE = "voice-to-content-simple-draft"
        const val JETPACK_AI_ERROR = "__JETPACK_AI_ERROR__"
    }

    suspend fun execute(
        siteModel: SiteModel,
        file: File
    ): VoiceToContentResult =
        withContext(Dispatchers.IO) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                return@withContext NetworkUnavailable
            }

            val transcriptionResponse = jetpackAIStore.fetchJetpackAITranscription(
                siteModel,
                FEATURE,
                file
            )

            val transcribedText: String? = when(transcriptionResponse) {
                is JetpackAITranscriptionResponse.Success -> {
                    transcriptionResponse.model
                }
                is JetpackAITranscriptionResponse.Error -> {
                    logger.logError("${transcriptionResponse.type} ${transcriptionResponse.message}")
                    null
                }
            }

            transcribedText?.let { transcribed ->
                val response = jetpackAIStore.fetchJetpackAIQuery(
                    site = siteModel,
                    feature = FEATURE,
                    role = ROLE,
                    message = transcribed,
                    stream = false,
                    type = TYPE
                )

                when(response) {
                    is JetpackAIQueryResponse.Success -> {
                        val finalContent: String? = response.choices[0].message?.content
                        // __JETPACK_AI_ERROR__ is a special marker we ask GPT to add to the request when it can’t
                        // understand the request for any reason, so maybe something confused GPT on some requests.
                        if (finalContent == null || finalContent == JETPACK_AI_ERROR) {
                            // Send back the transcribed text here
                            logger.logError(JETPACK_AI_ERROR)
                            return@withContext Success(content = transcribed)
                        } else {
                            return@withContext Success(content = finalContent)
                        }
                    }

                    is JetpackAIQueryResponse.Error -> {
                        logger.logError("${response.type.name} - ${response.message}")
                        return@withContext Success(content = transcribed)
                    }
                }
            } ?: run {
                logger.logError("Unable to transcribe audio content")
                return@withContext RemoteRequestFailure
            }
        }
}

sealed class VoiceToContentResult {
    data class Success(
        val content: String
    ): VoiceToContentResult()

    sealed class Failure: VoiceToContentResult() {
        data object NetworkUnavailable: Failure()
        data object RemoteRequestFailure: Failure()
    }
}
