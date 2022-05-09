package org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BloggingRemindersRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {

    suspend fun fetchBloggingReminders(site: SiteModel): BloggingRemindersSettingsPayload<BloggingRemindersSettingsResponse> {
        val url = WPCOMV2.sites.site(site.siteId).blogging_prompts.settings.url
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mapOf(),
            BloggingRemindersSettingsResponse::class.java
        )
        return when (response) {
            is Success -> BloggingRemindersSettingsPayload(response.data)
            is Error -> BloggingRemindersSettingsPayload(response.error.toBloggingPromptsError())
        }
    }

//    suspend fun setBloggingReminders(site: SiteModel, reminders: BloggingReminders): CardsPayload<CardsResponse> {
//        val url = WPCOMV2.sites.site(site.siteId).dashboard.cards_data.url
//        val params = reminders.,a
//        val response = wpComGsonRequestBuilder.syncGetRequest(
//            this,
//            url,
//            params,
//            CardsResponse::class.java
//        )
//        return when (response) {
//            is Success -> CardsPayload(response.data)
//            is Error -> CardsPayload(response.error.toCardsError())
//        }
//    }

    data class BloggingRemindersSettingsResponse(
        @SerializedName("updated") val content: BloggingRemindersSettingsResponseContent
    ) {
        fun toBloggingReminders(localSiteId: Int) = content.toBloggingReminders(localSiteId)
    }

    data class BloggingRemindersSettingsResponseContent(
        @SerializedName("prompts_card_opted_in") val promptsCardOptedIn: Boolean,
        @SerializedName("prompts_reminders_opted_in") val promptsRemindersOptedIn: Boolean,
        @SerializedName("reminders_days") val reminderDays: Map<String, Boolean>,
        @SerializedName("reminders_time") val remindersTime: String
    ) {
        fun toBloggingReminders(localSiteId: Int) = BloggingReminders(
            localSiteId = localSiteId,
            promptCardOptedIn = promptsCardOptedIn,
            promptRemindersOptedIn = promptsRemindersOptedIn,
            monday = reminderDays["monday"] ?: false,
            tuesday = reminderDays["tuesday"] ?: false,
            wednesday = reminderDays["wednesday"] ?: false,
            thursday = reminderDays["thursday"] ?: false,
            friday = reminderDays["friday"] ?: false,
            saturday = reminderDays["saturday"] ?: false,
            sunday = reminderDays["sunday"] ?: false,

        )
    }

    data class BloggingRemindersSettingsPayload<T>(
        val response: T? = null
    ) : Payload<BloggingRemindersSettingsError>() {
        constructor(error: BloggingRemindersSettingsError) : this() {
            this.error = error
        }
    }

    class BloggingRemindersSettingsError(
        val type: BloggingRemindersSettingsErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class BloggingRemindersSettingsErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR,
        TIMEOUT
    }

    fun WPComGsonNetworkError.toBloggingPromptsError(): BloggingRemindersSettingsError {
        val type = when (type) {
            GenericErrorType.TIMEOUT -> BloggingRemindersSettingsErrorType.TIMEOUT
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.SERVER_ERROR,
            GenericErrorType.INVALID_SSL_CERTIFICATE,
            GenericErrorType.NETWORK_ERROR -> BloggingRemindersSettingsErrorType.API_ERROR
            GenericErrorType.PARSE_ERROR,
            GenericErrorType.NOT_FOUND,
            GenericErrorType.CENSORED,
            GenericErrorType.INVALID_RESPONSE -> BloggingRemindersSettingsErrorType.INVALID_RESPONSE
            GenericErrorType.HTTP_AUTH_ERROR,
            GenericErrorType.AUTHORIZATION_REQUIRED,
            GenericErrorType.NOT_AUTHENTICATED -> BloggingRemindersSettingsErrorType.AUTHORIZATION_REQUIRED
            GenericErrorType.UNKNOWN,
            null -> BloggingRemindersSettingsErrorType.GENERIC_ERROR
        }
        return BloggingRemindersSettingsError(type, message)
    }
}