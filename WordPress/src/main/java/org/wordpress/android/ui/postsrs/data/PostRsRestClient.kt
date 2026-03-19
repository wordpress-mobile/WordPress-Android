package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.postsrs.AuthorInfo
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.SiteUtils
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.PostFormat
import uniffi.wp_api.TermCreateParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.ThemeListParams
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeSupports
import uniffi.wp_api.ThemeSupportsData
import uniffi.wp_api.UserListParams
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class AuthorPage(
    val authors: List<AuthorInfo>,
    val nextPageParams: UserListParams?,
)

@Singleton
class PostRsRestClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
) {
    private val mediaUrlCache = ConcurrentHashMap<Long, String>()
    private val userNameCache = ConcurrentHashMap<Long, String>()
    private val categoryNameCache = ConcurrentHashMap<Long, String>()
    private val tagNameCache = ConcurrentHashMap<Long, String>()

    fun clearCaches() {
        mediaUrlCache.clear()
        userNameCache.clear()
        categoryNameCache.clear()
        tagNameCache.clear()
    }

    /**
     * Fetches media source URLs for the given [mediaIds] in a single
     * network call using the `include` parameter, returning a map of
     * media ID to Photon-optimised URL. IDs already in the local cache
     * are returned immediately without a network round-trip.
     *
     * @param widthDp target display width in dp for Photon resizing.
     *     Pass 0 to use the full screen width.
     */
    suspend fun fetchMediaUrls(
        site: SiteModel,
        mediaIds: List<Long>,
        widthDp: Int = 0,
    ): Map<Long, String> {
        val widthPx = if (widthDp > 0) {
            (widthDp * context.resources.displayMetrics.density)
                .toInt()
        } else {
            0
        }
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in mediaIds) {
            val cached = mediaUrlCache[id]
            if (cached != null) {
                result[id] = toPhotonUrl(site, cached, widthPx)
            } else {
                uncached.add(id)
            }
        }
        if (uncached.isEmpty()) return result

        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.media().listWithEditContext(
                MediaListParams(include = uncached)
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                for (media in response.response.data) {
                    mediaUrlCache[media.id] = media.sourceUrl
                    result[media.id] = toPhotonUrl(
                        site, media.sourceUrl, widthPx
                    )
                }
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchMediaUrls failed: $msg"
                )
            }
        }
        return result
    }

    /**
     * Fetches display names for the given [userIds] in a single network
     * call using the `include` parameter, returning a map of user ID to
     * display name. IDs already in the local cache are returned
     * immediately without a network round-trip.
     */
    suspend fun fetchUserDisplayNames(
        site: SiteModel,
        userIds: List<Long>
    ): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in userIds) {
            val cached = userNameCache[id]
            if (cached != null) result[id] = cached else uncached.add(id)
        }
        if (uncached.isEmpty()) return result

        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.users().listWithViewContext(
                UserListParams(include = uncached)
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                for (user in response.response.data) {
                    userNameCache[user.id] = user.name
                    result[user.id] = user.name
                }
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchUserDisplayNames failed: $msg"
                )
            }
        }
        return result
    }

    /**
     * Fetches term names for the given [termIds] in a single network
     * call using the `include` parameter, returning a map of term ID
     * to name. IDs already in the local cache are returned immediately
     * without a network round-trip.
     */
    suspend fun fetchTermNames(
        site: SiteModel,
        termIds: List<Long>,
        endpointType: TermEndpointType,
    ): Map<Long, String> {
        val cache = termCache(endpointType)
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in termIds) {
            val cached = cache[id]
            if (cached != null) result[id] = cached else uncached.add(id)
        }
        if (uncached.isEmpty()) return result

        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.terms().listWithViewContext(
                endpointType,
                TermListParams(include = uncached)
            )
        }
        when (response) {
            is WpRequestResult.Success -> {
                for (term in response.response.data) {
                    cache[term.id] = term.name
                    result[term.id] = term.name
                }
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchTermNames failed: $msg"
                )
            }
        }
        return result
    }

    /**
     * Fetches a page of users for the given site, returning an
     * [AuthorPage] with the authors and optional next-page params.
     * Results are also cached in [userNameCache].
     */
    suspend fun fetchSiteAuthors(
        site: SiteModel,
        params: UserListParams = UserListParams(
            include = emptyList(),
            perPage = AUTHORS_PER_PAGE
        ),
    ): AuthorPage {
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.users().listWithViewContext(params)
        }
        return when (response) {
            is WpRequestResult.Success -> {
                val authors =
                    response.response.data.map { user ->
                        userNameCache[user.id] = user.name
                        AuthorInfo(
                            id = user.id,
                            name = user.name
                        )
                    }
                AuthorPage(
                    authors = authors,
                    nextPageParams =
                        response.response.nextPageParams,
                )
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchSiteAuthors failed: $msg"
                )
                AuthorPage(
                    authors = emptyList(),
                    nextPageParams = null,
                )
            }
        }
    }

    /**
     * Fetches a single page of terms for the given
     * [endpointType]. Pass [nextPageParams] to fetch
     * subsequent pages. Also populates the name cache.
     */
    suspend fun fetchTermsPage(
        site: SiteModel,
        endpointType: TermEndpointType,
        search: String? = null,
        nextPageParams: TermListParams? = null,
    ): TermsPageResult {
        val cache = termCache(endpointType)
        val client = wpApiClientProvider.getWpApiClient(site)
        val params = nextPageParams ?: TermListParams(
            perPage = PER_PAGE,
            search = search,
        )
        val response = client.request {
            it.terms().listWithViewContext(
                endpointType, params
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                val terms = response.response.data
                for (term in terms) {
                    cache[term.id] = term.name
                }
                TermsPageResult(
                    terms = terms,
                    nextPageParams =
                        response.response.nextPageParams,
                )
            }
            else -> {
                val msg = (response
                    as? WpRequestResult.WpError<*>)
                    ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "fetchTermsPage failed: $msg"
                )
                throw TermsFetchException(msg)
            }
        }
    }

    data class TermsPageResult(
        val terms: List<AnyTermWithViewContext>,
        val nextPageParams: TermListParams?,
    )

    class TermsFetchException(message: String?) :
        Exception(message ?: "Failed to fetch terms")

    /**
     * Creates a new term and returns its ID, or null on
     * failure. Also populates the name cache.
     */
    suspend fun createTerm(
        site: SiteModel,
        endpointType: TermEndpointType,
        name: String,
        parentId: Long? = null,
    ): Long? {
        val cache = termCache(endpointType)
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.terms().create(
                endpointType,
                TermCreateParams(
                    name = name,
                    parent = parentId
                )
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                val term = response.response.data
                cache[term.id] = term.name
                term.id
            }
            else -> {
                val msg =
                    (response as? WpRequestResult.WpError<*>)
                        ?.errorMessage
                AppLog.w(
                    AppLog.T.POSTS,
                    "createTerm failed: $msg"
                )
                null
            }
        }
    }

    /**
     * Fetches the post formats supported by the site's active
     * theme. Returns [DEFAULT_POST_FORMATS] on failure or when
     * the theme does not declare format support.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchSitePostFormats(
        site: SiteModel,
    ): List<PostFormat> {
        return try {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request {
                it.themes().listWithViewContext(
                    ThemeListParams(
                        status = ThemeStatus.Active
                    )
                )
            }
            when (response) {
                is WpRequestResult.Success -> {
                    val theme =
                        response.response.data.firstOrNull()
                            ?: return DEFAULT_POST_FORMATS
                    val supports = theme.themeSupports
                        ?: return DEFAULT_POST_FORMATS
                    val data = supports[ThemeSupports.Formats]
                        ?: return DEFAULT_POST_FORMATS
                    val slugs =
                        (data as? ThemeSupportsData.VecString)
                            ?.v1
                            ?: return DEFAULT_POST_FORMATS
                    if (slugs.isEmpty()) {
                        return DEFAULT_POST_FORMATS
                    }
                    val formats = slugs
                        .map { slugToPostFormat(it) }
                        .toMutableList()
                    if (formats.none {
                            it is PostFormat.Standard
                        }
                    ) {
                        formats.add(0, PostFormat.Standard)
                    }
                    formats
                }
                else -> {
                    val msg =
                        (response
                            as? WpRequestResult.WpError<*>)
                            ?.errorMessage
                    AppLog.w(
                        AppLog.T.POSTS,
                        "fetchSitePostFormats failed: $msg"
                    )
                    DEFAULT_POST_FORMATS
                }
            }
        } catch (e: Exception) {
            AppLog.w(
                AppLog.T.POSTS,
                "fetchSitePostFormats exception: " +
                    "${e.message}"
            )
            DEFAULT_POST_FORMATS
        }
    }

    private fun slugToPostFormat(slug: String): PostFormat =
        when (slug) {
            "standard" -> PostFormat.Standard
            "aside" -> PostFormat.Aside
            "audio" -> PostFormat.Audio
            "chat" -> PostFormat.Chat
            "gallery" -> PostFormat.Gallery
            "image" -> PostFormat.Image
            "link" -> PostFormat.Link
            "quote" -> PostFormat.Quote
            "status" -> PostFormat.Status
            "video" -> PostFormat.Video
            else -> PostFormat.Custom(slug)
        }

    private fun toPhotonUrl(
        site: SiteModel,
        sourceUrl: String,
        widthPx: Int = 0,
    ): String {
        if (!SiteUtils.isPhotonCapable(site)) return sourceUrl
        val width = if (widthPx > 0) {
            widthPx
        } else {
            context.resources.displayMetrics.widthPixels
        }
        return PhotonUtils.getPhotonImageUrl(
            sourceUrl, width, 0, site.isPrivateWPComAtomic
        )
    }

    private fun termCache(
        endpointType: TermEndpointType,
    ): ConcurrentHashMap<Long, String> =
        if (endpointType is TermEndpointType.Categories) {
            categoryNameCache
        } else {
            tagNameCache
        }

    companion object {
        internal const val AUTHORS_PER_PAGE: UInt = 20u
        private const val PER_PAGE = 100u

        val DEFAULT_POST_FORMATS = listOf(
            PostFormat.Standard,
            PostFormat.Aside,
            PostFormat.Audio,
            PostFormat.Chat,
            PostFormat.Gallery,
            PostFormat.Image,
            PostFormat.Link,
            PostFormat.Quote,
            PostFormat.Status,
            PostFormat.Video,
        )
    }
}
