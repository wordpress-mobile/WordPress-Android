package org.wordpress.android.ui.voicetocontent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIQueryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.viewmodel.ContextProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class VoiceToContentUseCase @Inject constructor(
    private val jetpackAIStore: JetpackAIStore,
    private val fileHelperWrapper: VoiceToContentTempFileHelperWrapper
) {
    companion object {
        const val FEATURE = "voice_to_content"
        const val ROLE = "jetpack-ai"
        const val TYPE = "voice-to-content-simple-draft"
        const val JETPACK_AI_ERROR = "__JETPACK_AI_ERROR__"
    }

    suspend fun execute(
        siteModel: SiteModel,
    ): VoiceToContentResult =
        withContext(Dispatchers.IO) {
            val file = fileHelperWrapper.getAudioFile() ?: return@withContext VoiceToContentResult(isError = true)
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
                    null
                }
            }

            transcribedText?.let {
                val response = jetpackAIStore.fetchJetpackAIQuery(
                    site = siteModel,
                    feature = FEATURE,
                    role = ROLE,
                    message = it,
                    stream = false,
                    type = TYPE
                )

                when(response) {
                    is JetpackAIQueryResponse.Success -> {
                        val finalContent: String = response.choices[0].message.content
                        // __JETPACK_AI_ERROR__ is a special marker we ask GPT to add to the request when it can’t
                        // understand the request for any reason, so maybe something confused GPT on some requests.
                        if (finalContent == JETPACK_AI_ERROR) {
                            return@withContext VoiceToContentResult(isError = true)
                        } else {
                            return@withContext VoiceToContentResult(content = response.choices[0].message.content)
                        }
                    }

                    is JetpackAIQueryResponse.Error -> {
                        return@withContext VoiceToContentResult(isError = true)
                    }
                }
            } ?:return@withContext VoiceToContentResult(isError = true)
        }
}

// todo: build out the result object
data class VoiceToContentResult(
    val content: String? = null,
    val isError: Boolean = false
)

// todo: Remove this class when real impl is in place - it's here so I can start unit tests
class VoiceToContentTempFileHelperWrapper @Inject constructor(
    private val contextProvider: ContextProvider
) {
    fun getAudioFile(): File? {
        val result = runCatching {
            getFileFromAssets(contextProvider.getContext())
        }

        return result.getOrElse {
            null
        }
    }

    // todo: Do not forget to delete the test file from the asset directory
    private fun getFileFromAssets(context: Context): File {
        val fileName = "jetpack-ai-transcription-test-audio-file.m4a"
        val file = File(context.filesDir, fileName)
        context.assets.open(fileName).use { inputStream ->
            copyInputStreamToFile(inputStream, file)
        }
        return file
    }

    private fun copyInputStreamToFile(inputStream: InputStream, outputFile: File) {
        FileOutputStream(outputFile).use { outputStream ->
            val buffer = ByteArray(KILO_BYTE)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
        }
        inputStream.close()
    }
    companion object {
        const val KILO_BYTE = 1024
    }
}
