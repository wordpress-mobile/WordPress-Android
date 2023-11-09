package org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV3
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptResponse
import org.wordpress.android.fluxc.store.Store.OnChangedError
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BloggingPromptsRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchPrompts(
        site: SiteModel,
        perPage: Int,
        after: Date,
        ignoresYear: Boolean = true,
    ): BloggingPromptsPayload<BloggingPromptsListResponse> {
        val url = WPCOMV3.sites.site(site.siteId).blogging_prompts.url
        val params = mutableMapOf(
            "per_page" to perPage.toString(),
            "after" to BloggingPromptsUtils.dateToString(after, ignoresYear)
        )
        if (ignoresYear) {
            // when ignoring the year we force the response to be for the current year
            // we also need to use order desc to get the latest prompt for each date, even though
            // the list of actual prompts is ordered ascending by date
            params["force_year"] = BloggingPromptsUtils.yearForDate(after)
            params["order"] = "desc"
        }
        val response = wpComGsonRequestBuilder.syncGetRequest<BloggingPromptsListResponse>(
            this,
            url,
            params,
            BloggingPromptsListResponseTypeToken.type
        )
        return when (response) {
            is Success -> BloggingPromptsPayload(response.data)
            is Error -> BloggingPromptsPayload(response.error.toBloggingPromptsError())
        }
    }

    data class BloggingPromptResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("text") val text: String,
        @SerializedName("date") val date: String,
        @SerializedName("answered") val isAnswered: Boolean,
        @SerializedName("attribution") val attribution: String,
        @SerializedName("answered_users_count") val respondentsCount: Int,
        @SerializedName("answered_users_sample") val respondentsAvatars: List<BloggingPromptsRespondentAvatar>,
        @SerializedName("answered_link") val answeredLink: String,
        @SerializedName("answered_link_text") val answeredLinkText: String,
        @SerializedName("bloganuary_id") val bloganuaryId: String? = null,
    ) {
        fun toBloggingPromptModel() = BloggingPromptModel(
            id = id,
            text = text,
            date = BloggingPromptsUtils.stringToDate(date),
            isAnswered = isAnswered,
            attribution = attribution,
            respondentsCount = respondentsCount,
            respondentsAvatarUrls = respondentsAvatars.map { it.avatarUrl },
            answeredLink = answeredLink,
            bloganuaryId = bloganuaryId,
        )
    }

    data class BloggingPromptsRespondentAvatar(
        @SerializedName("avatar") val avatarUrl: String
    )
}

data class BloggingPromptsPayload<T>(
    val response: T? = null
) : Payload<BloggingPromptsError>() {
    constructor(error: BloggingPromptsError) : this() {
        this.error = error
    }
}

class BloggingPromptsError(
    val type: BloggingPromptsErrorType,
    val message: String? = null
) : OnChangedError

enum class BloggingPromptsErrorType {
    GENERIC_ERROR,
    AUTHORIZATION_REQUIRED,
    INVALID_RESPONSE,
    API_ERROR,
    TIMEOUT
}

fun WPComGsonNetworkError.toBloggingPromptsError(): BloggingPromptsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> BloggingPromptsErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> BloggingPromptsErrorType.API_ERROR

        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> BloggingPromptsErrorType.INVALID_RESPONSE

        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> BloggingPromptsErrorType.AUTHORIZATION_REQUIRED

        GenericErrorType.UNKNOWN,
        null -> BloggingPromptsErrorType.GENERIC_ERROR
    }
    return BloggingPromptsError(type, message)
}

typealias BloggingPromptsListResponse = List<BloggingPromptResponse>

object BloggingPromptsListResponseTypeToken : TypeToken<BloggingPromptsListResponse>()

fun BloggingPromptsListResponse.toBloggingPrompts() = map { it.toBloggingPromptModel() }
