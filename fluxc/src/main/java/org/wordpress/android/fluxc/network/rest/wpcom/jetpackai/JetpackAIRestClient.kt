package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.utils.extensions.putIfNotNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackAIRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    companion object {
        private const val FIELDS_TO_REQUEST = "completion"
    }

    suspend fun fetchJetpackAIJWTToken(
        site: SiteModel
    ) : JetpackAIJWTTokenResponse {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_openai_query.jwt.url
        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = null,
            clazz = JetpackAIJWTTokenDto::class.java,
        )

        return when (response) {
            is Response.Success -> JetpackAIJWTTokenResponse.Success(JWTToken(response.data.token))
            is Response.Error -> JetpackAIJWTTokenResponse.Error(
                response.error.toJetpackAICompletionsError(),
                response.error.message
            )
        }
    }

    suspend fun fetchJetpackAITextCompletion(
        token: JWTToken,
        prompt: String,
        feature: String,
        format: ResponseFormat? = null,
        model: String? = null
    ): JetpackAICompletionsResponse {
        val url = WPCOMV2.text_completion.url
        val body = mutableMapOf<String, String>()
        body.apply {
            put("token", token.value)
            put("prompt", prompt)
            put("feature", feature)
            put("_fields", FIELDS_TO_REQUEST)
            putIfNotNull("response_format" to format?.value, "model" to model)
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
            clazz = JetpackAITextCompletionDto::class.java
        )

        return when (response) {
            is Response.Success -> JetpackAICompletionsResponse.Success(response.data.completion)
            is Response.Error -> JetpackAICompletionsResponse.Error(
                response.error.toJetpackAICompletionsError(),
                response.error.message
            )
        }
    }

    /**
     * Fetches Jetpack AI completions for a given prompt.
     *
     * @param site      The site for which completions are fetched.
     * @param prompt    The prompt used to generate completions.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param skipCache If true, bypasses the default 30-second throttle and fetches fresh data.
     * @param postId    Optional post ID to mark its content as generated by Jetpack AI. If provided,
     *                  a post meta`_jetpack_ai_calls` is added or updated, indicating the number
     *                  of times AI is used in the post. Not required if marking is not needed.
     */
    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String,
        feature: String? = null,
        skipCache: Boolean = false,
        postId: Long? = null,
    ): JetpackAICompletionsResponse {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_ai.completions.url
        val body = mutableMapOf<String, Any>()
        body.apply {
            put("content", prompt)
            postId?.let { put("post_id", it) }
            put("skip_cache", skipCache)
            feature?.let { put("feature", it) }
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
            clazz = String::class.java
        )

        return when (response) {
            is Response.Success -> JetpackAICompletionsResponse.Success(response.data)
            is Response.Error -> JetpackAICompletionsResponse.Error(
                response.error.toJetpackAICompletionsError(),
                response.error.message
            )
        }
    }

    /**
     * Fetches Jetpack AI Query for a given message.
     *
     * @param jwtToken  The jwt authorization token.
     * @param message   The message to be expanded by the Jetpack AI BE.
     * @param role      A special marker to indicate that the message needs to be expanded by the Jetpack AI BE.
     * @param type      An indication of which kind of post-processing action will be executed over the content.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param stream    When true, the response is a set of EventSource events, otherwise a single response
     */
    @Suppress("LongParameterList")
    suspend fun fetchJetpackAiQuery(
        jwtToken: JWTToken,
        message: String,
        role: String,
        type: String,
        feature: String?,
        stream: Boolean
    ): JetpackAIQueryResponse {
        val url = WPCOMV2.jetpack_ai_query.url

        val body = mutableMapOf<String, Any>().apply {
            put("messages", createJetpackAIQueryMessage(text = message, role=role, type = type))
            put("stream", stream)
            putIfNotNull("feature" to feature)
        }

        val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = mapOf("token" to jwtToken.value),
            body = body,
            clazz = JetpackAIQueryDto::class.java,
        )

        return when (response) {
            is Response.Success -> {
                (response.data as? JetpackAIQueryDto)?.toJetpackAIQueryResponse()
                    ?: JetpackAIQueryResponse.Error(JetpackAIQueryErrorType.INVALID_DATA, "Can not get the object")
            }
            is Response.Error -> {
                JetpackAIQueryResponse.Error(
                    response.error.toJetpackAIQueryError(),
                    response.error.message
                )
            }
        }
    }

    /**
     * Fetches Jetpack AI Assistant feature for site
     *
     * @param site  The SiteModel for which the Jetpack AI Assistant feature is fetched
     */
    @Suppress("LongParameterList")
    suspend fun fetchJetpackAiAssistantFeature(
        site: SiteModel
    ): JetpackAIAssistantFeatureResponse {
        val url = WPCOMV2.sites.site(site.siteId).jetpack_ai.ai_assistant_feature.url

        val response = wpComGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            params = emptyMap(),
            clazz = JetpackAIAssistantFeatureDto::class.java,
        )

        return when (response) {
            is Response.Success -> {
                JetpackAIAssistantFeatureResponse.Success(
                    response.data.toJetpackAIAssistantFeature()
                )
            }
            is Response.Error -> {
                JetpackAIAssistantFeatureResponse.Error(
                    response.error.toJetpackAIAssistantFeatureError(),
                    response.error.message
                )
            }
        }
    }

    internal  data class JetpackAIJWTTokenDto(
        @SerializedName ("success") val success: Boolean,
        @SerializedName("token") val token: String
    )

    internal data class JetpackAITextCompletionDto(
        @SerializedName ("completion") val completion: String
    )

    internal data class JetpackAIQueryDto(val model: String, val choices: List<Choice>) {
        data class Choice(val index: Int, val message: Message) {
            data class Message(val role: String, val content: String)
        }
    }
    sealed class JetpackAIJWTTokenResponse {
        data class Success(val token: JWTToken) : JetpackAIJWTTokenResponse()
        data class Error(
            val type: JetpackAICompletionsErrorType,
            val message: String? = null
        ) : JetpackAIJWTTokenResponse()
    }

    sealed class JetpackAICompletionsResponse {
        data class Success(val completion: String) : JetpackAICompletionsResponse()
        data class Error(
            val type: JetpackAICompletionsErrorType,
            val message: String? = null
        ) : JetpackAICompletionsResponse()
    }

    enum class JetpackAICompletionsErrorType {
        API_ERROR,
        AUTH_ERROR,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        TIMEOUT,
        NETWORK_ERROR
    }

    sealed class JetpackAIQueryResponse {
        data class Success(val model: String, val choices: List<Choice>) : JetpackAIQueryResponse() {
            data class Choice(val index: Int, val message: Message) {
                data class Message(val role: String, val content: String)
            }
        }

        data class Error(
            val type: JetpackAIQueryErrorType,
            val message: String? = null
        ) : JetpackAIQueryResponse()
    }

    private fun JetpackAIQueryDto.toJetpackAIQueryResponse(): JetpackAIQueryResponse {
        return JetpackAIQueryResponse.Success(model, choices.map { choice ->
            JetpackAIQueryResponse.Success.Choice(
                choice.index,
                JetpackAIQueryResponse.Success.Choice.Message(
                    choice.message.role,
                    choice.message.content
                )
            )
        })
    }

    enum class JetpackAIQueryErrorType {
        API_ERROR,
        AUTH_ERROR,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        TIMEOUT,
        NETWORK_ERROR,
        INVALID_DATA
    }

    private fun createJetpackAIQueryMessage(text: String, type: String, role: String) =
        listOf(mapOf("context" to mapOf("type" to type,"content" to text),"role" to role))

    enum class ResponseFormat(val value: String) {
        JSON("json_object"),
        TEXT("text")
    }

    private fun WPComGsonNetworkError.toJetpackAICompletionsError() =
        when (type) {
            GenericErrorType.TIMEOUT -> JetpackAICompletionsErrorType.TIMEOUT
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.INVALID_SSL_CERTIFICATE,
            GenericErrorType.NETWORK_ERROR -> JetpackAICompletionsErrorType.NETWORK_ERROR
            GenericErrorType.SERVER_ERROR -> JetpackAICompletionsErrorType.API_ERROR
            GenericErrorType.PARSE_ERROR,
            GenericErrorType.NOT_FOUND,
            GenericErrorType.CENSORED,
            GenericErrorType.INVALID_RESPONSE -> JetpackAICompletionsErrorType.INVALID_RESPONSE
            GenericErrorType.HTTP_AUTH_ERROR,
            GenericErrorType.AUTHORIZATION_REQUIRED,
            GenericErrorType.NOT_AUTHENTICATED -> JetpackAICompletionsErrorType.AUTH_ERROR
            GenericErrorType.UNKNOWN -> JetpackAICompletionsErrorType.GENERIC_ERROR
            null -> JetpackAICompletionsErrorType.GENERIC_ERROR
        }
    private fun WPComGsonNetworkError.toJetpackAIQueryError() =
        when (type) {
            GenericErrorType.TIMEOUT -> JetpackAIQueryErrorType.TIMEOUT
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.INVALID_SSL_CERTIFICATE,
            GenericErrorType.NETWORK_ERROR -> JetpackAIQueryErrorType.NETWORK_ERROR
            GenericErrorType.SERVER_ERROR -> JetpackAIQueryErrorType.API_ERROR
            GenericErrorType.PARSE_ERROR,
            GenericErrorType.NOT_FOUND,
            GenericErrorType.CENSORED,
            GenericErrorType.INVALID_RESPONSE -> JetpackAIQueryErrorType.INVALID_RESPONSE
            GenericErrorType.HTTP_AUTH_ERROR,
            GenericErrorType.AUTHORIZATION_REQUIRED,
            GenericErrorType.NOT_AUTHENTICATED -> JetpackAIQueryErrorType.AUTH_ERROR
            GenericErrorType.UNKNOWN -> JetpackAIQueryErrorType.GENERIC_ERROR
            null -> JetpackAIQueryErrorType.GENERIC_ERROR
        }
    private fun WPComGsonNetworkError.toJetpackAIAssistantFeatureError() =
        when (type) {
            GenericErrorType.TIMEOUT -> JetpackAIAssistantFeatureErrorType.TIMEOUT
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.INVALID_SSL_CERTIFICATE,
            GenericErrorType.NETWORK_ERROR -> JetpackAIAssistantFeatureErrorType.NETWORK_ERROR
            GenericErrorType.SERVER_ERROR -> JetpackAIAssistantFeatureErrorType.API_ERROR
            GenericErrorType.PARSE_ERROR,
            GenericErrorType.NOT_FOUND,
            GenericErrorType.CENSORED,
            GenericErrorType.INVALID_RESPONSE -> JetpackAIAssistantFeatureErrorType.INVALID_RESPONSE
            GenericErrorType.HTTP_AUTH_ERROR,
            GenericErrorType.AUTHORIZATION_REQUIRED,
            GenericErrorType.NOT_AUTHENTICATED -> JetpackAIAssistantFeatureErrorType.AUTH_ERROR
            GenericErrorType.UNKNOWN -> JetpackAIAssistantFeatureErrorType.GENERIC_ERROR
            null -> JetpackAIAssistantFeatureErrorType.GENERIC_ERROR
        }
}
