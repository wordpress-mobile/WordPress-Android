package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.postsrs.AuthorInfo
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.SiteUtils
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.MediaDetailsPayload
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.MediaWithEditContext
import uniffi.wp_api.PostFormat
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.TermCreateParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.SparseThemeFieldWithViewContext
import uniffi.wp_api.SparseThemeWithViewContext
import uniffi.wp_api.ThemeListParams
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeSupports
import uniffi.wp_api.ThemeSupportsData
import uniffi.wp_api.UserListParams
import java.util.Collections
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
    /**
     * Keyed by site and media ID, since media IDs only mean anything
     * within a site. Bounded and least-recently-used, so scrolling a
     * long list can't grow it without limit.
     */
    private val mediaImageCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, MediaImage>(
            MEDIA_CACHE_CAPACITY, MEDIA_CACHE_LOAD_FACTOR, true
        ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, MediaImage>,
            ): Boolean = size > MEDIA_CACHE_MAX_ENTRIES
        }
    )
    private val userNameCache = ConcurrentHashMap<Long, String>()
    private val categoryNameCache = ConcurrentHashMap<Long, String>()
    private val tagNameCache = ConcurrentHashMap<Long, String>()

    private val displayMetrics get() = context.resources.displayMetrics

    fun clearCaches() {
        mediaImageCache.clear()
        userNameCache.clear()
        categoryNameCache.clear()
        tagNameCache.clear()
    }

    /**
     * A URL for [mediaId] sized to fill the screen's width, or null if it could not be resolved.
     * A cached id is answered without a network round-trip.
     */
    suspend fun fetchMediaUrl(site: SiteModel, mediaId: Long): String? =
        fetchMediaImages(site, listOf(mediaId))[mediaId]?.toDisplayUrl(
            SiteUtils.getAccessibilityInfoFromSite(site),
            SiteUtils.isAccessedViaWPComRest(site),
            displayMetrics.widthPixels,
            heightPx = 0,
        )

    /**
     * URLs for both shapes a list row can draw a featured image at - a [thumbnailDp] square and a
     * hero banner [heroHeightDp] tall across the full width. Both come off the same media object,
     * so the row can pick its shape at draw time without the second one costing a request.
     */
    suspend fun fetchFeaturedImageUrls(
        site: SiteModel,
        mediaIds: List<Long>,
        thumbnailDp: Int,
        heroHeightDp: Int,
    ): Map<Long, FeaturedImageUrls> {
        val accessibilityInfo = SiteUtils.getAccessibilityInfoFromSite(site)
        val isWpComRest = SiteUtils.isAccessedViaWPComRest(site)
        val thumbnailPx = DisplayUtils.dpToPx(context, thumbnailDp)
        val heroHeightPx = DisplayUtils.dpToPx(context, heroHeightDp)
        val heroWidthPx = displayMetrics.widthPixels
        return fetchMediaImages(site, mediaIds).mapValues { (_, image) ->
            FeaturedImageUrls(
                thumbnail = image.toDisplayUrl(
                    accessibilityInfo, isWpComRest, thumbnailPx, thumbnailPx
                ),
                hero = image.toDisplayUrl(
                    accessibilityInfo, isWpComRest, heroWidthPx, heroHeightPx
                ),
            )
        }
    }

    /**
     * Resolves [mediaIds] to their media objects, hitting the network only for uncached ones. Ids
     * left out of the result could not be resolved and the caller should stop waiting on them.
     */
    private suspend fun fetchMediaImages(
        site: SiteModel,
        mediaIds: List<Long>,
    ): Map<Long, MediaImage> {
        val resolved = mutableMapOf<Long, MediaImage>()
        val uncached = mutableListOf<Long>()
        for (id in mediaIds) {
            val cached = mediaImageCache[mediaCacheKey(site, id)]
            if (cached != null) resolved[id] = cached else uncached.add(id)
        }
        if (uncached.isEmpty()) return resolved

        val client = wpApiClientProvider.getWpApiClient(site)
        // `include` doesn't lift the page size (default 10), so a batch bigger than a page is
        // sent in page-sized chunks. Without this the ids beyond the first page would come back
        // unanswered and be recorded as unresolvable.
        for (chunk in uncached.chunked(PER_PAGE.toInt())) {
            resolved += fetchMediaChunk(site, client, chunk)
        }
        return resolved
    }

    /**
     * One chunk's media, or empty if the request could not be answered. Retried once if the failure
     * looks transient, since losing the chunk costs every image in it until the list reloads.
     */
    private suspend fun fetchMediaChunk(
        site: SiteModel,
        client: WpApiClient,
        chunk: List<Long>,
    ): Map<Long, MediaImage> {
        var attemptsLeft = MEDIA_ATTEMPTS
        while (attemptsLeft > 0) {
            val response = client.request {
                it.media().listWithEditContext(
                    MediaListParams(include = chunk, perPage = PER_PAGE)
                )
            }
            // Mapped here rather than returned raw: the mediaDetails handle belongs to the
            // response and has to be read while it is still alive.
            if (response is WpRequestResult.Success) {
                return response.response.data.associate { media ->
                    val image = media.toMediaImage()
                    mediaImageCache[mediaCacheKey(site, media.id)] = image
                    media.id to image
                }
            }
            // The wordpress-rs client already logged the status, method and URL through
            // wpRsErrorLogger; this only names which call it was.
            AppLog.w(AppLog.T.POSTS, "fetchMediaImages failed")
            attemptsLeft = if (response.isTransient()) attemptsLeft - 1 else 0
            if (attemptsLeft > 0) delay(MEDIA_RETRY_DELAY_MS)
        }
        return emptyMap()
    }

    /**
     * Whether a second attempt could plausibly succeed: a 5xx, a timeout or a dropped connection.
     * Auth, permission, not-found, parse and rate-limit failures come back the same every time.
     */
    private fun WpRequestResult<*>.isTransient(): Boolean = when (this) {
        is WpRequestResult.RequestExecutionFailed ->
            reason is RequestExecutionErrorReason.ConnectionError ||
                reason is RequestExecutionErrorReason.HttpTimeoutError ||
                statusCode.isServerError()
        is WpRequestResult.WpError -> statusCode.isServerError()
        is WpRequestResult.InvalidHttpStatusCode -> statusCode.isServerError()
        is WpRequestResult.UnknownError -> statusCode.isServerError()
        else -> false
    }

    private fun UInt?.isServerError(): Boolean = this != null && this in HTTP_SERVER_ERRORS

    /**
     * Fetches display names for the given [userIds] in one network call
     * per page-sized batch, returning a map of user ID to
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
        // Chunked for the same reason as fetchMediaImages: `include` doesn't lift the page size.
        for (chunk in uncached.chunked(PER_PAGE.toInt())) {
            val response = client.request {
                it.users().listWithViewContext(
                    UserListParams(include = chunk, perPage = PER_PAGE)
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
        // The REST API caps results per page (defaulting to 10), so request a larger page size
        // and follow the pagination params until every requested term has been fetched.
        // Otherwise a post with more than one page of assigned terms would lose the names past
        // the first page.
        var params: TermListParams? = TermListParams(
            include = uncached,
            perPage = PER_PAGE,
        )
        while (params != null) {
            val currentParams = params
            val response = client.request {
                it.terms().listWithViewContext(
                    endpointType, currentParams
                )
            }
            when (response) {
                is WpRequestResult.Success -> {
                    for (term in response.response.data) {
                        cache[term.id] = term.name
                        result[term.id] = term.name
                    }
                    params = response.response.nextPageParams
                }
                else -> {
                    val msg =
                        (response as? WpRequestResult.WpError<*>)
                            ?.errorMessage
                    AppLog.w(
                        AppLog.T.POSTS,
                        "fetchTermNames failed: $msg"
                    )
                    params = null
                }
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
    suspend fun fetchSitePostFormats(
        site: SiteModel,
    ): List<PostFormat> {
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.themes().filterListWithViewContext(
                ThemeListParams(
                    status = ThemeStatus.Active
                ),
                listOf(
                    SparseThemeFieldWithViewContext
                        .THEME_SUPPORTS
                )
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                parsePostFormats(response.response.data)
                    ?: DEFAULT_POST_FORMATS
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
    }

    private fun parsePostFormats(
        themes: List<SparseThemeWithViewContext>,
    ): List<PostFormat>? {
        val slugs =
            themes.firstOrNull()
                ?.themeSupports
                ?.get(ThemeSupports.Formats)
                ?.let { it as? ThemeSupportsData.VecString }
                ?.v1
                ?.takeIf { it.isNotEmpty() }
                ?: return null
        return (listOf(PostFormat.Standard) +
            slugs.map { slugToPostFormat(it) })
            .distinct()
    }

    private fun slugToPostFormat(slug: String): PostFormat =
        SLUG_TO_FORMAT[slug] ?: PostFormat.Custom(slug)

    private fun mediaCacheKey(site: SiteModel, mediaId: Long): String =
        "${site.id}:$mediaId"

    /**
     * Reads the source URL and the available renders off the media
     * object. The `mediaDetails` handle is owned by the response, so
     * this has to be called while the response is still alive.
     */
    private fun MediaWithEditContext.toMediaImage(): MediaImage {
        val details = (mediaDetails.parseAsMimeType(mimeType)
            as? MediaDetailsPayload.Image)?.v1
        val sizes = details?.sizes.orEmpty()
            .map { (_, size) ->
                ScaledSize(
                    size.width.toInt(),
                    size.height.toInt(),
                    size.sourceUrl,
                )
            }
            .sortedBy { it.width }
        return MediaImage(
            sourceUrl = sourceUrl,
            sourceWidth = details?.width?.toInt() ?: 0,
            sourceHeight = details?.height?.toInt() ?: 0,
            sizes = sizes,
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

        private const val MEDIA_CACHE_MAX_ENTRIES = 500
        private const val MEDIA_CACHE_CAPACITY = 64
        private const val MEDIA_CACHE_LOAD_FACTOR = 0.75f

        private const val MEDIA_ATTEMPTS = 2
        private const val MEDIA_RETRY_DELAY_MS = 500L
        private val HTTP_SERVER_ERRORS = 500u..599u

        private val SLUG_TO_FORMAT = mapOf(
            "standard" to PostFormat.Standard,
            "aside" to PostFormat.Aside,
            "audio" to PostFormat.Audio,
            "chat" to PostFormat.Chat,
            "gallery" to PostFormat.Gallery,
            "image" to PostFormat.Image,
            "link" to PostFormat.Link,
            "quote" to PostFormat.Quote,
            "status" to PostFormat.Status,
            "video" to PostFormat.Video,
        )

        val DEFAULT_POST_FORMATS =
            SLUG_TO_FORMAT.values.toList()
    }
}
