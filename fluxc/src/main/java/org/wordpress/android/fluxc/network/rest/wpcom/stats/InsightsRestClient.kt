package org.wordpress.android.fluxc.network.rest.wpcom.stats

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.network.utils.getFormattedDate
import org.wordpress.android.fluxc.store.InsightsStore.FetchInsightsPayload
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import java.util.Date
import javax.inject.Singleton

@Singleton
class InsightsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean): FetchInsightsPayload<AllTimeResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.urlV1_1

        val params = mapOf<String, String>()
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                AllTimeResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean): FetchInsightsPayload<MostPopularResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.insights.urlV1_1

        val params = mapOf<String, String>()
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                MostPopularResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchLatestPostForInsights(site: SiteModel, forced: Boolean): FetchInsightsPayload<PostsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).posts.urlV1_1
        val params = mapOf(
                "order_by" to "date",
                "number" to "1",
                "type" to "post",
                "fields" to "ID,title,URL,discussion,like_count,date"
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                PostsResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchPostStats(
        site: SiteModel,
        postId: Long,
        forced: Boolean
    ): FetchInsightsPayload<PostStatsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.post.item(postId).urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                mapOf(),
                PostStatsResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchTimePeriodStats(
        site: SiteModel,
        period: StatsGranularity,
        date: Date,
        forced: Boolean
    ): FetchInsightsPayload<VisitResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1

        val params = mapOf(
                "unit" to period.toPath(),
                "quantity" to "1",
                "date" to getFormattedDate(site, date, period)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                VisitResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchFollowers(
        site: SiteModel,
        type: FollowerType,
        pageSize: Int = 6,
        forced: Boolean
    ): FetchInsightsPayload<FollowersResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.followers.urlV1_1

        val params = mapOf(
                "type" to type.path,
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                FollowersResponse::class.java,
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchTopComments(
        site: SiteModel,
        pageSize: Int = 6,
        forced: Boolean
    ): FetchInsightsPayload<CommentsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.comments.urlV1_1

        val params = mapOf(
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CommentsResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchTopCommentingFollowers(
        site: SiteModel,
        pageSize: Int = 6,
        forced: Boolean
    ): FetchInsightsPayload<CommentingFollowersResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.comment_followers.urlV1_1

        val params = mapOf(
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CommentingFollowersResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    private fun buildStatsError(error: WPComGsonNetworkError): StatsError {
        val type = when (error.type) {
            TIMEOUT -> StatsErrorType.TIMEOUT
            NO_CONNECTION,
            SERVER_ERROR,
            INVALID_SSL_CERTIFICATE,
            NETWORK_ERROR -> StatsErrorType.API_ERROR
            PARSE_ERROR,
            NOT_FOUND,
            CENSORED,
            INVALID_RESPONSE -> StatsErrorType.INVALID_RESPONSE
            HTTP_AUTH_ERROR,
            AUTHORIZATION_REQUIRED,
            NOT_AUTHENTICATED -> StatsErrorType.AUTHORIZATION_REQUIRED
            UNKNOWN,
            null -> GENERIC_ERROR
        }
        return StatsError(type, error.message)
    }

    data class AllTimeResponse(
        @SerializedName("date") var date: Date? = null,
        @SerializedName("stats") val stats: StatsResponse
    ) {
        data class StatsResponse(
            @SerializedName("visitors") val visitors: Int,
            @SerializedName("views") val views: Int,
            @SerializedName("posts") val posts: Int,
            @SerializedName("views_best_day") val viewsBestDay: String,
            @SerializedName("views_best_day_total") val viewsBestDayTotal: Int
        )
    }

    data class MostPopularResponse(
        @SerializedName("highest_day_of_week") val highestDayOfWeek: Int,
        @SerializedName("highest_hour") val highestHour: Int,
        @SerializedName("highest_day_percent") val highestDayPercent: Double,
        @SerializedName("highest_hour_percent") val highestHourPercent: Double
    )

    data class PostsResponse(
        @SerializedName("found") val postsFound: Int = 0,
        @SerializedName("posts") val posts: List<PostResponse> = listOf()
    ) {
        data class PostResponse(
            @SerializedName("ID") val id: Long,
            @SerializedName("title") val title: String,
            @SerializedName("date") val date: Date,
            @SerializedName("URL") val url: String,
            @SerializedName("like_count") val likeCount: Int,
            @SerializedName("discussion") val discussion: Discussion?
        ) {
            data class Discussion(
                @SerializedName("comment_count") val commentCount: Int
            )
        }
    }

    data class PostStatsResponse(
        @SerializedName("highest_month") val highestMonth: Int = 0,
        @SerializedName("highest_day_average") val highestDayAverage: Int = 0,
        @SerializedName("highest_week_average") val highestWeekAverage: Int = 0,
        @SerializedName("views") val views: Int,
        @SerializedName("date") val date: String? = null,
        @SerializedName("data") val data: List<List<String>>?,
        @SerializedName("fields") val fields: List<String>?,
        @SerializedName("weeks") val weeks: List<Week>,
        @SerializedName("years") val years: Map<Int, Year>,
        @SerializedName("averages") val averages: Map<Int, Average>

    ) {
        data class Year(
            @SerializedName("months") val months: Map<Int, Int>,
            @SerializedName("total") val total: Int
        )

        data class Week(
            @SerializedName("average") val average: Int,
            @SerializedName("total") val total: Int,
            @SerializedName("days") val days: List<Day>
        )

        data class Day(
            @SerializedName("day") val day: String,
            @SerializedName("count") val count: Int
        )

        data class Average(
            @SerializedName("months") val months: Map<Int, Int>,
            @SerializedName("overall") val overall: Int
        )
    }

    data class VisitResponse(
        @SerializedName("date") val date: String,
        @SerializedName("unit") val unit: String,
        @SerializedName("fields") val fields: List<String>,
        @SerializedName("data") val data: List<List<String>>
    )

    data class CommentsResponse(
        @SerializedName("date") val date: String,
        @SerializedName("monthly_comments") val monthlyComments: Int,
        @SerializedName("total_comments") val totalComments: Int,
        @SerializedName("most_active_day") val mostActiveDay: String,
        @SerializedName("most_commented_post") val mostCommentedPost: Post,
        @SerializedName("authors") val authors: List<Author>,
        @SerializedName("posts") val posts: List<Post>
    ) {
        data class Author(
            @SerializedName("name") val name: String,
            @SerializedName("link") val link: String,
            @SerializedName("gravatar") val gravatar: String,
            @SerializedName("comments") val comments: Int,
            @SerializedName("follow_data") val followData: FollowData
        )

        data class Post(
            @SerializedName("name") val name: String,
            @SerializedName("link") val link: String,
            @SerializedName("id") val id: Int,
            @SerializedName("comments") val comments: Int
        )
    }

    data class CommentingFollowersResponse(
        @SerializedName("page") val page: Int,
        @SerializedName("pages") val pages: Int,
        @SerializedName("total") val total: Int,
        @SerializedName("posts") val posts: List<Post>
    ) {
        data class Post(
            @SerializedName("name") val name: String,
            @SerializedName("link") val link: String,
            @SerializedName("id") val id: Int,
            @SerializedName("comments") val comments: Int
        )
    }

    /*

        setGroupId(authorJSON.getString("name"));
        setName(authorJSON.getString("name"));
        setViews(authorJSON.getInt("views"));
        setAvatar(JSONUtils.getString(authorJSON, "avatar"));
        {"sites":[{"ID":122536325,"name":"Jetpack Site 1","description":"Just another WordPress site","URL":"http:\/\/do.wpmt.co\/jp-1","user_can_manage":false,"capabilities":{"edit_pages":true,"edit_posts":true,"edit_others_posts":true,"edit_others_pages":true,"delete_posts":true,"delete_others_posts":true,"edit_theme_options":true,"edit_users":false,"list_users":true,"manage_categories":true,"manage_options":true,"moderate_comments":true,"activate_wordads":true,"promote_users":true,"publish_posts":true,"upload_files":true,"delete_users":false,"remove_users":true,"view_stats":true},"jetpack":true,"is_multisite":false,"post_count":3,"subscribers_count":0,"lang":"en-US","icon":{"img":"http:\/\/do.wpmt.co\/jp-1\/wp-content\/uploads\/2017\/03\/cropped-cropped-cropped-pony-4.jpeg","ico":"http:\/\/do.wpmt.co\/jp-1\/wp-content\/uploads\/2017\/03\/cropped-cropped-cropped-pony-4.jpeg?w=16","media_id":14},"logo":{"id":0,"sizes":[],"url":""},"visible":true,"is_private":false,"single_user_site":true,"is_vip":false,"is_following":false,"options":{"timezone":"","gmt_offset":0,"blog_public":0,"videopress_enabled":false,"upgraded_filetypes_enabled":true,"login_url":"http:\/\/do.wpmt.co\/jp-1\/wp-login.php","admin_url":"http:\/\/do.wpmt.co\/jp-1\/wp-admin\/","is_mapped_domain":true,"is_redirect":false,"unmapped_url":"http:\/\/do.wpmt.co\/jp-1","featured_images_enabled":false,"theme_slug":"edin-wpcom","header_image":false,"background_color":false,"image_default_link_type":"none","image_thumbnail_width":150,"image_thumbnail_height":150,"image_thumbnail_crop":0,"image_medium_width":300,"image_medium_height":300,"image_large_width":1024,"image_large_height":1024,"permalink_structure":"\/%year%\/%monthnum%\/%day%\/%postname%\/","post_formats":{"aside":"Aside","image":"Image","video":"Video","quote":"Quote","link":"Link","gallery":"Gallery","status":"Status","audio":"Audio","chat":"Chat"},"default_post_format":"0","default_category":1,"allowed_file_types":["jpg","jpeg","png","gif","pdf","doc","ppt","odt","pptx","docx","pps","ppsx","xls","xlsx","key"],"show_on_front":"posts","default_likes_enabled":true,"default_sharing_status":false,"default_comment_status":true,"default_ping_status":true,"software_version":"4.8.7","created_at":"2017-01-13T14:06:27+00:00","wordads":false,"publicize_permanently_disabled":false,"frame_nonce":"0368b4a9aa","headstart":false,"headstart_is_fresh":false,"ak_vp_bundle_enabled":0,"advanced_seo_front_page_description":"","advanced_seo_title_formats":[],"verification_services_codes":null,"podcasting_archive":null,"is_domain_only":false,"is_automated_transfer":false,"is_wpcom_store":false,"woocommerce_is_active":false,"design_type":null,"site_goals":null,"jetpack_version":"5.9","main_network_site":"http:\/\/do.wpmt.co\/jp-1","active_modules":["after-the-deadline","contact-form","custom-content-types","custom-css","enhanced-distribution","gravatar-hovercards","json-api","latex","manage","notes","post-by-email","protect","publicize","sharedaddy","shortcodes","shortlinks","sitemaps","stats","subscriptions","verification-tools","widget-visibility","widgets"],"max_upload_size":false,"wp_memory_limit":"40M","wp_max_memory_limit":"256M","is_multi_network":false,"is_multi_site":false,"file_mod_disabled":false},"plan":{"product_id":2002,"product_slug":"jetpack_free","product_name_short":"Free","free_trial":false,"expired":false,"user_is_owner":false,"is_free":true,"features":{"active":["akismet","support"],"available":{"akismet":["jetpack_premium","jetpack_business","jetpack_premium_monthly","jetpack_business_monthly"],"vaultpress-backups":["jetpack_premium","jetpack_business","jetpack_premium_monthly","jetpack_business_monthly"],"vaultpress-backup-archive":["jetpack_premium","jetpack_business","jetpack_premium_monthly","jetpack_business_monthly"],"vaultpress-storage-space":["jetpack_premium","jetpack_business","jetpack_premium_monthly","jetpack_business_monthly"],"vaultpress-automated-restores":["jetpack_premium","jetpack_business","jetpack_premium_monthly","jetpack_business_monthly"],"si
        {"date":"2018-11-07","authors":[{"name":"A WordPress Commenter","link":"?s=wapuu@wordpress.example","gravatar":"https:\/\/1.gravatar.com\/avatar\/d7a973c7dab26985da5f961be7b74480?s=64&amp;d=https%3A%2F%2F1.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D64&amp;r=G","comments":"1","follow_data":null}],"posts":[{"name":"Hello world!","link":"http:\/\/do.wpmt.co\/jp-1\/2017\/01\/13\/hello-world\/","id":"1","comments":"1"}],"monthly_comments":0,"total_comments":1,"most_active_day":"2017-01-13 13:07:09","most_active_time":"13:00","most_commented_post":{"name":"Hello world!","link":"http:\/\/do.wpmt.co\/jp-1\/2017\/01\/13\/hello-world\/","id":"1","comments":"1"}}
        {"date":"2018-11-07","authors":[{"name":"A WordPress Commenter","link":"?s=wapuu@wordpress.example","gravatar":"https:\/\/1.gravatar.com\/avatar\/d7a973c7dab26985da5f961be7b74480?s=64&amp;d=https%3A%2F%2F1.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D64&amp;r=G","comments":"1","follow_data":null}],"posts":[{"name":"Hello world!","link":"http:\/\/do.wpmt.co\/jp-1\/2017\/01\/13\/hello-world\/","id":"1","comments":"1"}],"monthly_comments":0,"total_comments":1,"most_active_day":"2017-01-13 13:07:09","most_active_time":"13:00","most_commented_post":{"name":"Hello world!","link":"http:\/\/do.wpmt.co\/jp-1\/2017\/01\/13\/hello-world\/","id":"1","comments":"1"}}
        {"page":1,"pages":0,"total":0,"posts":[]}

     */

    private fun StatsGranularity.toPath(): String {
        return when (this) {
            DAYS -> "day"
            WEEKS -> "week"
            MONTHS -> "month"
            YEARS -> "year"
        }
    }

    enum class FollowerType(val path: String) {
        EMAIL("email"), WP_COM("wpcom")
    }

    data class FollowersResponse(
        @SerializedName("page") val page: Int,
        @SerializedName("pages") val pages: Int,
        @SerializedName("total") val total: Int,
        @SerializedName("total_email") val totalEmail: Int,
        @SerializedName("total_wpcom") val totalWpCom: Int,
        @SerializedName("subscribers") val subscribers: List<FollowerResponse>

    ) {
        data class FollowerResponse(
            @SerializedName("label") val label: String,
            @SerializedName("avatar") val avatar: String,
            @SerializedName("url") val url: String,
            @SerializedName("date_subscribed") val dateSubscribed: Date,
            @SerializedName("follow_data") val followData: FollowData
        )
    }

    data class FollowData(
        @SerializedName("type") val type: String,
        @SerializedName("params") val params: FollowParams
    ) {
        data class FollowParams(
            @SerializedName("follow-text") val followText: String,
            @SerializedName("following-text") val followingText: String,
            @SerializedName("following-hover-text") val followingHoverText: String,
            @SerializedName("is_following") val isFollowing: Boolean,
            @SerializedName("blog_id") val blogId: String,
            @SerializedName("site_id") val siteId: String,
            @SerializedName("stats-source") val statsSource: String,
            @SerializedName("blog_domain") val blogDomain: String
        )
    }
}
