package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.postsrs.AuthorInfo
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.reader.utils.SiteAccessibilityInfo
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SiteUtils
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.MediaDetailsPayload
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.MediaWithEditContext
import uniffi.wp_api.PostFormat
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
import kotlin.math.abs

data class AuthorPage(
    val authors: List<AuthorInfo>,
    val nextPageParams: UserListParams?,
)

/** One of the renders WordPress generated for an image at upload time. */
private data class ScaledSize(
    val width: Int,
    val height: Int,
    val url: String,
)

private data class MediaImage(
    val sourceUrl: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    /** Renders from `media_details.sizes`, ascending by width. Empty for non-images. */
    val sizes: List<ScaledSize>,
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

    fun clearCaches() {
        mediaImageCache.clear()
        userNameCache.clear()
        categoryNameCache.clear()
        tagNameCache.clear()
    }

    /**
     * Fetches the given [mediaIds] in a single network call using the
     * `include` parameter, returning a map of media ID to a URL sized
     * for display. IDs already in the local cache are returned
     * immediately without a network round-trip.
     *
     * @param widthDp target display width in dp. Pass 0 to use the
     *     full screen width.
     * @param displayAspect width/height the image will be displayed
     *     at, when the caller crops to a fixed shape. Lets a render
     *     WordPress cropped to that same shape be used - a square
     *     thumbnail for a square slot, say. Leave null to accept only
     *     renders that still match the original's proportions.
     */
    suspend fun fetchMediaUrls(
        site: SiteModel,
        mediaIds: List<Long>,
        widthDp: Int = 0,
        displayAspect: Float? = null,
    ): Map<Long, String> {
        val widthPx = if (widthDp > 0) {
            (widthDp * context.resources.displayMetrics.density)
                .toInt()
        } else {
            context.resources.displayMetrics.widthPixels
        }
        val accessibilityInfo =
            SiteUtils.getAccessibilityInfoFromSite(site)
        val isWpComRest = SiteUtils.isAccessedViaWPComRest(site)
        val result = mutableMapOf<Long, String>()
        val uncached = mutableListOf<Long>()
        for (id in mediaIds) {
            val cached = mediaImageCache[mediaCacheKey(site, id)]
            if (cached != null) {
                result[id] = toDisplayUrl(
                    accessibilityInfo, isWpComRest, cached, widthPx,
                    displayAspect
                )
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
                    val image = media.toMediaImage()
                    mediaImageCache[mediaCacheKey(site, media.id)] =
                        image
                    result[media.id] = toDisplayUrl(
                        accessibilityInfo, isWpComRest, image, widthPx,
                        displayAspect
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

    /**
     * The smallest render at least [targetWidth] wide that we can use
     * without losing content. Themes register hard-cropped sizes
     * (WordPress crops `thumbnail` by default), and handing one of
     * those to a screen that crops again would quietly cut the image
     * down twice - so a render only qualifies if it still matches the
     * original's proportions, or if it was cropped to the very shape
     * the caller is about to display it at ([displayAspect]).
     */
    private fun MediaImage.renderAtLeast(
        targetWidth: Int,
        displayAspect: Float?,
    ): String? {
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        val sourceRatio = sourceWidth.toFloat() / sourceHeight
        return sizes.firstOrNull {
            it.usableWidthFor(displayAspect) >= targetWidth && (
                it.matchesRatio(sourceRatio) ||
                    (displayAspect != null && it.matchesRatio(displayAspect))
                )
        }?.url
    }

    /**
     * How much of this render's width survives being cropped to
     * [displayAspect]. Cropping to a shape narrower than the render
     * is limited by its height, so a wide render carries far less
     * detail into a square slot than its own width suggests - a
     * 300x169 thumbnail of a 16:9 photo only has 169px to give.
     */
    private fun ScaledSize.usableWidthFor(displayAspect: Float?): Int {
        if (displayAspect == null || height <= 0) return width
        return if (width.toFloat() / height > displayAspect) {
            (height * displayAspect).toInt()
        } else {
            width
        }
    }

    private fun ScaledSize.matchesRatio(ratio: Float): Boolean =
        height > 0 &&
            abs(width.toFloat() / height - ratio) <=
            ratio * ASPECT_TOLERANCE

    /**
     * Picks a URL to display [image] at [widthPx]. Photon-capable
     * sites resize the original server-side. Everywhere else - self
     * hosted sites in particular - we ask for the smallest render
     * WordPress already generated that's at least as wide as we need,
     * so a 64dp thumbnail doesn't pull down the full-size upload.
     * Never picks a render narrower than the target, so images can't
     * end up pixelated.
     */
    private fun toDisplayUrl(
        accessibilityInfo: SiteAccessibilityInfo,
        isWpComRest: Boolean,
        image: MediaImage,
        widthPx: Int,
        displayAspect: Float?,
    ): String {
        val url = if (accessibilityInfo.isPhotonCapable) {
            image.sourceUrl
        } else {
            image.renderAtLeast(widthPx, displayAspect)
                ?: image.sourceUrl
        }
        // Only WP.com-hosted media honors ?w=, and only Photon needs
        // the rewrite. Self-hosted ignores the param, and rewriting
        // the URL there would drop any signed or CDN query string it
        // carries, breaking the image outright.
        return if (isWpComRest) {
            ReaderUtils.getResizedImageUrl(
                url, widthPx, 0, accessibilityInfo
            )
        } else {
            url
        }
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

        /**
         * How far a render's aspect ratio may drift from the original
         * before we treat it as cropped rather than scaled. Generous
         * enough for rounding, far tighter than any real crop.
         */
        private const val ASPECT_TOLERANCE = 0.05f

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
