package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.utils.JetpackAITranscriptionUtils
import org.wordpress.android.fluxc.utils.WPComRestClientUtils.getHttpUrlWithLocale
import org.wordpress.android.util.AppLog
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named

class JetpackAITranscriptionRestClient  @Inject constructor(
    private val appContext: Context,
    private val userAgent: UserAgent,
    @Named("regular") private val okHttpClient: OkHttpClient,
    private val jetpackAIUtils: JetpackAITranscriptionUtils
) {
    companion object {
        private const val DEFAULT_AUDIO_FILE_SIZE_LIMIT: Long = 25 * 1024 * 1024 // 25 MB
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun fetchJetpackAITranscription(
        jwtToken: JWTToken,
        feature: String?,
        audioFile: File,
        audioFileSizeLimit: Long = DEFAULT_AUDIO_FILE_SIZE_LIMIT
    ) : JetpackAITranscriptionResponse {
        if (!jetpackAIUtils.isFileEligibleForTranscription(audioFile, audioFileSizeLimit)) {
            JetpackAITranscriptionResponse.Error(
                JetpackAITranscriptionErrorType.INELIGIBLE_AUDIO_FILE)
        }

        val url = WPCOMV2.jetpack_ai_transcription.url
        val requestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart(
                "audio_file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType()))
            feature?.let { addFormDataPart("feature", it) }
        }.build()

        val request = Request.Builder().apply {
            getHttpUrlWithLocale(appContext, url)?.let {
                url(it)
            } ?: url(url)
            addHeader("Authorization", "Bearer ${jwtToken.value}")
            addHeader("User-Agent",  userAgent.toString())
            post(requestBody)
        }.build()

        val result = runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMessage = response.message.takeIf { msg -> msg.isNotBlank() }
                            ?: "Unknown okHttpClient error ${response.code}"
                    return@use JetpackAITranscriptionResponse.Error(
                        fromHttpStatusCode(response.code),
                        errorMessage
                    )
                } else {
                    val body = response.body?.use {
                        it.string()
                    }

                    val dto = try {
                        val gson = GsonBuilder()
                            .registerTypeAdapter(
                                JetpackAITranscriptionDto::class.java,
                                JetpackAITranscriptionDeserializer()
                            )
                            .create()
                        gson.fromJson(body, JetpackAITranscriptionDto::class.java)
                    } catch (e: Exception) {
                        // Handle all potential exceptions gracefully to prevent the app from crashing.
                        // Possible exceptions include:
                        // - JsonSyntaxException: Thrown when the JSON is not in the expected format.
                        // - ParseException: Thrown during date parsing or other parsing operations.
                        // - IllegalStateException: Thrown when an operation is not called at an appropriate time.
                        // All exceptions are logged for debugging purposes, but
                        // return null to ensure the app continues to run smoothly.
                        AppLog.e(AppLog.T.API, "Failed to parse transcription response: $e")
                        null
                    }
                    return@use dto.toJetpackAITranscriptionResponse()
                }
            }
        }

        return result.getOrElse {
            val errorMessage = it.message?.takeIf { msg -> msg.isNotBlank() }
                ?: "Unknown error of type ${it::class.java.simpleName}"
            JetpackAITranscriptionResponse.Error(
                fromThrowable(it), errorMessage)
        }
    }

    internal data class JetpackAITranscriptionDto(
        val text: String? = null,
        val code: String? = null,
        val message: String? = null,
        val data: JetpackAITranscriptionErrorDto? = null
    )

    internal data class JetpackAITranscriptionErrorDto(
        val status: Int
    )

    private fun JetpackAITranscriptionDto?.toJetpackAITranscriptionResponse():
        JetpackAITranscriptionResponse {
        return when (this) {
            null -> {
                JetpackAITranscriptionResponse.Error(
                    JetpackAITranscriptionErrorType.PARSE_ERROR,
                    "Unable to parse transcription response"
                )
            }

            else -> this.toResponse()
        }
    }

    private fun JetpackAITranscriptionDto.toResponse(): JetpackAITranscriptionResponse {
        return when {
            text != null -> JetpackAITranscriptionResponse.Success(text)
            data != null -> JetpackAITranscriptionResponse.Error(
                fromHttpStatusCode(data.status),
                "Error while handling response $code $message"
            )
            else -> JetpackAITranscriptionResponse.Error(
                JetpackAITranscriptionErrorType.GENERIC_ERROR,
                "Invalid response"
            )
        }
    }

    internal class JetpackAITranscriptionDeserializer : JsonDeserializer<JetpackAITranscriptionDto>
    {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): JetpackAITranscriptionDto {
            val jsonObject = json?.asJsonObject ?: throw JsonParseException("Invalid JSON")

            return if (jsonObject.has("text")) {
                JetpackAITranscriptionDto(text = jsonObject.get("text").asString)
            } else if (jsonObject.has("code") &&
                jsonObject.has("message") && jsonObject.has("data")) {
                val data: JetpackAITranscriptionErrorDto? =
                    context?.deserialize(
                        jsonObject.get("data"),
                        JetpackAITranscriptionErrorDto::class.java
                    )
                JetpackAITranscriptionDto(
                    code = jsonObject.get("code").asString,
                    message = jsonObject.get("message").asString,
                    data = data
                )
            } else {
                throw JsonParseException("Unknown JSON structure")
            }
        }
    }

    @Suppress("MagicNumber")
    private fun fromHttpStatusCode(code: Int): JetpackAITranscriptionErrorType {
        AppLog.e(AppLog.T.API, "Failed transcription http status: $code")
        return when (code) {
            400 -> JetpackAITranscriptionErrorType.BAD_REQUEST
            401 -> JetpackAITranscriptionErrorType.AUTH_ERROR
            404 -> JetpackAITranscriptionErrorType.NOT_FOUND
            403 -> JetpackAITranscriptionErrorType.NOT_AUTHENTICATED
            413 -> JetpackAITranscriptionErrorType.REQUEST_TOO_LARGE
            429 -> JetpackAITranscriptionErrorType.TOO_MANY_REQUESTS
            500 -> JetpackAITranscriptionErrorType.SERVER_ERROR
            503 -> JetpackAITranscriptionErrorType.JETPACK_AI_SERVICE_UNAVAILABLE
            else -> JetpackAITranscriptionErrorType.GENERIC_ERROR
        }
    }

    private fun fromThrowable(e: Throwable): JetpackAITranscriptionErrorType {
        AppLog.e(AppLog.T.API, "Failed transcription network response: $e")
        return if (e is IOException) {
            when (e) {
                is SocketTimeoutException -> JetpackAITranscriptionErrorType.TIMEOUT
                is ConnectException,
                is UnknownHostException -> JetpackAITranscriptionErrorType.CONNECTION_ERROR
                else -> JetpackAITranscriptionErrorType.GENERIC_ERROR
            }
        } else {
            JetpackAITranscriptionErrorType.GENERIC_ERROR
        }
    }
}
