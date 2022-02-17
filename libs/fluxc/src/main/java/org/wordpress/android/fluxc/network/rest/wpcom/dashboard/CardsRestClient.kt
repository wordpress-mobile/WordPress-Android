package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CardsRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchCards(site: SiteModel): CardsPayload<CardsResponse> {
        val url = WPCOMV2.sites.site(site.siteId).dashboard.cards_data.url
        val params = buildDashboardCardsParams()
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CardsResponse::class.java
        )
        return when (response) {
            is Success -> CardsPayload(response.data)
            is Error -> CardsPayload(response.error.toCardsError())
        }
    }

    private fun buildDashboardCardsParams() = mapOf(CARDS to listOf(POSTS, TODAYS_STATS).joinToString(","))

    data class CardsResponse(
        @SerializedName("todays_stats") val todaysStats: TodaysStatsResponse? = null,
        @SerializedName("posts") val posts: PostsResponse? = null
    ) {
        fun toCards() = arrayListOf<CardModel>().apply {
            todaysStats?.let { add(it.toTodaysStatsCard()) }
            posts?.let { add(it.toPosts()) }
        }.toList()
    }

    data class TodaysStatsResponse(
        @SerializedName("views") val views: Int? = null,
        @SerializedName("visitors") val visitors: Int? = null,
        @SerializedName("likes") val likes: Int? = null,
        @SerializedName("comments") val comments: Int? = null
    ) {
        fun toTodaysStatsCard() = TodaysStatsCardModel(
                views = views ?: 0,
                visitors = visitors ?: 0,
                likes = likes ?: 0,
                comments = comments ?: 0
        )
    }

    data class PostsResponse(
        @SerializedName("has_published") val hasPublished: Boolean,
        @SerializedName("draft") val draft: List<PostResponse>,
        @SerializedName("scheduled") val scheduled: List<PostResponse>
    ) {
        fun toPosts() = PostsCardModel(
                hasPublished = hasPublished,
                draft = draft.map { it.toPost() },
                scheduled = scheduled.map { it.toPost() }
        )
    }

    data class PostResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("title") val title: String,
        @SerializedName("content") val content: String,
        @SerializedName("featured_image") val featuredImage: String?,
        @SerializedName("date") val date: String
    ) {
        fun toPost() = PostCardModel(
                id = id,
                title = title,
                content = content,
                featuredImage = featuredImage,
                date = CardsUtils.fromDate(date)
        )
    }

    companion object {
        private const val CARDS = "cards"
        private const val POSTS = "posts"
        private const val TODAYS_STATS = "todays_stats"
    }
}

fun WPComGsonNetworkError.toCardsError(): CardsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> CardsErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> CardsErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> CardsErrorType.INVALID_RESPONSE
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> CardsErrorType.AUTHORIZATION_REQUIRED
        GenericErrorType.UNKNOWN,
        null -> CardsErrorType.GENERIC_ERROR
    }
    return CardsError(type, message)
}
