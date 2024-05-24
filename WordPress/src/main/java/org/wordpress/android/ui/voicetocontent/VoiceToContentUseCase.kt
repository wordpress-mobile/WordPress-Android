package org.wordpress.android.ui.voicetocontent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionRestClient
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.viewmodel.ContextProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class VoiceToContentUseCase @Inject constructor(
    private val jetpackAIStore: JetpackAIStore,
    private val contextProvider: ContextProvider
) {
    companion object {
        const val FEATURE = "voice_to_content"
        private const val KILO_BYTE = 1024
    }

    suspend fun execute(
        siteModel: SiteModel,
    ): VoiceToContentResult =
        withContext(Dispatchers.IO) {
            val file = getAudioFile() ?: return@withContext VoiceToContentResult(isError = true)
            val response = jetpackAIStore.fetchJetpackAITranscription(
                siteModel,
                FEATURE,
                file
            )

            when(response) {
                is JetpackAITranscriptionRestClient.JetpackAITranscriptionResponse.Success -> {
                    return@withContext VoiceToContentResult(content = response.model)
                }
                is JetpackAITranscriptionRestClient.JetpackAITranscriptionResponse.Error -> {
                    return@withContext VoiceToContentResult(isError = true)
                }
            }
        }

    // todo: The next three methods are temporary to support development - remove when the real impl is in place
    private fun getAudioFile(): File? {
        val result = runCatching {
            getFileFromAssets(contextProvider.getContext())
        }

        return result.getOrElse {
            null
        }
    }

    // todo: Do not forget to delete the test file from the asset directory - when the real impl is in place
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
}

// todo: build out the result object
data class VoiceToContentResult(
    val content: String? = null,
    val isError: Boolean = false
)
