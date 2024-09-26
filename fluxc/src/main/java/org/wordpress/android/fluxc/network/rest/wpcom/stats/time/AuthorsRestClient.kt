package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthorsRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val gson: Gson,
    private val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchAuthors(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        itemsToLoad: Int,
        forced: Boolean
    ): FetchStatsPayload<AuthorsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.top_authors.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "max" to itemsToLoad.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                AuthorsResponse::class.java,
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                response.data.groups.values.forEach { it.authors.forEach { group -> group.build(gson) } }
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    data class AuthorsResponse(
        @SerializedName("period") val statsGranularity: String?,
        @SerializedName("days") val groups: Map<String, Groups>
    ) {
        data class Groups(
            @SerializedName("other_views") val otherViews: Int?,
            @SerializedName("authors") val authors: List<Author>
        )

        @Suppress("DataClassShouldBeImmutable")
        data class Author(
            @SerializedName("name") val name: String?,
            @SerializedName("views") var views: Int?,
            @SerializedName("avatar") val avatarUrl: String?,
            @SerializedName("posts") val posts: JsonElement?,
            @SerializedName("mappedPosts") var mappedPosts: List<Post>? = null
        ) {
            fun build(gson: Gson) {
                when (this.posts) {
                    is JsonArray -> this.mappedPosts = this.posts.map {
                        gson.fromJson<Post>(
                                it,
                                Post::class.java
                        )
                    }
                    is JsonObject -> this.views = this.posts.getInt("views")
                }
            }
        }

        data class Post(
            @SerializedName("id") val postId: String?,
            @SerializedName("title") val title: String?,
            @SerializedName("views") val views: Int?,
            @SerializedName("url") val url: String?
        )
    }
}
