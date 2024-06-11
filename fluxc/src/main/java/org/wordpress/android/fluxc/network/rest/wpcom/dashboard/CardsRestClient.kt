package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import android.content.Context
import android.os.Build
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.ActivityCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.CardOrder
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.DynamicCardRowModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PagesCardModel.PageCardModel
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
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient.ActivitiesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsPayload
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardErrorType
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
    suspend fun fetchCards(payload: FetchCardsPayload): CardsPayload<CardsResponse> {
        val url = WPCOMV2.sites.site(payload.site.siteId).dashboard.cards_data.url
        val params = buildDashboardCardsParams(payload)
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

    private fun buildDashboardCardsParams(payload: FetchCardsPayload) = mapOf(
        CARDS to payload.cardTypes.joinToString(",") { it.label },
        "build_number" to payload.buildNumber,
        "device_id" to payload.deviceId,
        "identifier" to payload.identifier,
        "marketing_version" to payload.marketingVersion,
        "platform" to payload.platform,
        "os_version" to payload.osVersion,
        )

    data class FetchCardsPayload(
        val site: SiteModel,
        val cardTypes: List<CardModel.Type>,
        val buildNumber: String,
        val deviceId: String,
        val identifier: String,
        val marketingVersion: String,
        val platform: String,
        val osVersion: String = Build.VERSION.RELEASE,
    )

    data class CardsResponse(
        @SerializedName("todays_stats") val todaysStats: TodaysStatsResponse? = null,
        @SerializedName("posts") val posts: PostsResponse? = null,
        @SerializedName("pages") val pages: List<PageResponse>? = null,
        @SerializedName("activity") val activity: ActivitiesResponse? = null,
        @SerializedName("dynamic") val dynamic: List<DynamicCardResponse>? = null,
    ) {
        fun toCards() = arrayListOf<CardModel>().apply {
            todaysStats?.let { add(it.toTodaysStatsCard()) }
            posts?.let { add(it.toPosts()) }
            pages?.let { add(getPagesCardModel(it))}
            activity?.let { add(it.toActivityCardModel()) }
            dynamic?.let { add(getDynamicCardsModel(it)) }
        }.toList()

        private fun getPagesCardModel(pages: List<PageResponse>): PagesCardModel {
            return PagesCardModel(pages.map{ it.toPages() })
        }

        private fun getDynamicCardsModel(dynamicCards: List<DynamicCardResponse>): DynamicCardsModel {
            return DynamicCardsModel(dynamicCards.map { it.toDynamicCard() })
        }
    }

    data class TodaysStatsResponse(
        @SerializedName("views") val views: Int? = null,
        @SerializedName("visitors") val visitors: Int? = null,
        @SerializedName("likes") val likes: Int? = null,
        @SerializedName("comments") val comments: Int? = null,
        @SerializedName("error") val error: String? = null
    ) {
        fun toTodaysStatsCard() = TodaysStatsCardModel(
                views = views ?: 0,
                visitors = visitors ?: 0,
                likes = likes ?: 0,
                comments = comments ?: 0,
                error = error?.let { toTodaysStatsCardsError(it) }
        )

        private fun toTodaysStatsCardsError(error: String): TodaysStatsCardError {
            val errorType = when (error) {
                JETPACK_DISCONNECTED -> TodaysStatsCardErrorType.JETPACK_DISCONNECTED
                JETPACK_DISABLED -> TodaysStatsCardErrorType.JETPACK_DISABLED
                UNAUTHORIZED -> TodaysStatsCardErrorType.UNAUTHORIZED
                else -> TodaysStatsCardErrorType.GENERIC_ERROR
            }
            return TodaysStatsCardError(errorType, error)
        }
    }

    data class PostsResponse(
        @SerializedName("has_published") val hasPublished: Boolean? = null,
        @SerializedName("draft") val draft: List<PostResponse>? = null,
        @SerializedName("scheduled") val scheduled: List<PostResponse>? = null,
        @SerializedName("error") val error: String? = null
    ) {
        fun toPosts() = PostsCardModel(
                hasPublished = hasPublished ?: false,
                draft = draft?.map { it.toPost() } ?: emptyList(),
                scheduled = scheduled?.map { it.toPost() } ?: emptyList(),
                error = error?.let { toPostCardError(it) }
        )

        private fun toPostCardError(error: String): PostCardError {
            val errorType = when (error) {
                UNAUTHORIZED -> PostCardErrorType.UNAUTHORIZED
                else -> PostCardErrorType.GENERIC_ERROR
            }
            return PostCardError(errorType, error)
        }
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

    data class PageResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("title") val title: String,
        @SerializedName("content") val content: String,
        @SerializedName("modified") val modified: String,
        @SerializedName("status") val status: String,
        @SerializedName("date") val date: String
    ){
        fun toPages() = PageCardModel(
                id = id,
                title = title,
                content = content,
                lastModifiedOrScheduledOn = CardsUtils.fromDate(modified),
                status = status,
                date = CardsUtils.fromDate(date)
        )
    }

    data class DynamicCardResponse(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String?,
        @SerializedName("featured_image") val featuredImage: String?,
        @SerializedName("url") val url: String?,
        @SerializedName("action") val action: String?,
        @SerializedName("order") val order: String?,
        @SerializedName("rows") val rows: List<DynamicCardRowResponse>?,
    ) {
        fun toDynamicCard() = DynamicCardModel(
            id = id,
            title = title,
            featuredImage = featuredImage,
            url = url,
            action = action,
            order = CardOrder.fromString(order),
            rows = rows?.map { it.toDynamicCardRow() } ?: emptyList()
        )
    }

    data class DynamicCardRowResponse(
        @SerializedName("icon") val icon: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
    ) {
        fun toDynamicCardRow() = DynamicCardRowModel(
            icon = icon,
            title = title,
            description = description
        )
    }

    companion object {
        private const val CARDS = "cards"
        private const val JETPACK_DISCONNECTED = "jetpack_disconnected"
        private const val JETPACK_DISABLED = "jetpack_disabled"
        const val UNAUTHORIZED = "unauthorized"
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

fun ActivitiesResponse.toActivityCardModel(): ActivityCardModel {
    val error = error?.let { toActivityCardError(it) }

    val activities = current?.orderedItems?.mapNotNull {
        when {
            it.activity_id == null || it.summary == null || it.content?.text == null ||
            it.published == null -> {
                null
            }
            else -> {
                ActivityLogModel(
                    activityID = it.activity_id,
                    summary = it.summary,
                    content = it.content,
                    name = it.name,
                    type = it.type,
                    gridicon = it.gridicon,
                    status = it.status,
                    rewindable = it.is_rewindable,
                    rewindID = it.rewind_id,
                    published = it.published,
                    actor = it.actor?.let { act ->
                        ActivityLogModel.ActivityActor(
                            act.name,
                            act.type,
                            act.wpcom_user_id,
                            act.icon?.url,
                            act.role
                        )
                    }
                )
            }
        }
    }

    return ActivityCardModel(
        activities = activities ?: emptyList(),
        error = error
    )
}
fun toActivityCardError(error: String): ActivityCardError {
    val errorType = when (error) {
        CardsRestClient.UNAUTHORIZED -> ActivityCardErrorType.UNAUTHORIZED
        else -> ActivityCardErrorType.GENERIC_ERROR
    }
    return ActivityCardError(errorType, error)
}
