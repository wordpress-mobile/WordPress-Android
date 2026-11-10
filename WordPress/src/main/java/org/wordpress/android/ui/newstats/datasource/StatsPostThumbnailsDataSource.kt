package org.wordpress.android.ui.newstats.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.rs.data.RsSiteRestClient
import uniffi.wp_api.PostEndpointType
import javax.inject.Inject
import javax.inject.Named

/**
 * Resolves the featured image thumbnails shown on the Posts & Pages detail screen.
 *
 * The stats top-posts response carries a title, a view count and an item type, but no image, so the
 * images have to come from the site's own REST API. Doing that per post would be two calls each --
 * the post for its featured image id, then the media object for a URL. Instead this batches: one
 * call per endpoint the list actually touches, plus one for all the media at once, so a screenful
 * of posts costs two or three requests rather than twenty.
 *
 * Only posts and pages are looked up. The homepage and attachments have no featured image of their
 * own, so they never reach the network and simply keep their glyph.
 *
 * A failure anywhere costs thumbnails, not the screen: it comes back as a missing entry, and the
 * row falls back to the type glyph it was already showing.
 */
class StatsPostThumbnailsDataSource @Inject constructor(
    private val rsSiteRestClient: RsSiteRestClient,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Thumbnail URLs for [items] that have a featured image, keyed by post id. Ids left out of the
     * result have no thumbnail to show.
     *
     * Runs on [ioDispatcher]: the wordpress-rs client dispatches each request itself, but maps the
     * response on whichever thread called it, and that mapping parses every media object's details
     * through the Rust bindings. The other callers of these lookups wrap them the same way.
     */
    suspend fun fetchThumbnails(
        site: SiteModel,
        items: List<StatsThumbnailRequest>
    ): Map<Long, String> = withContext(ioDispatcher) {
        val byEndpoint = items
            .mapNotNull { item -> item.postType.toEndpointType()?.let { it to item.postId } }
            .groupBy({ it.first }, { it.second })

        // Post id -> featured media id, across both endpoints. An item type with no featured image
        // of its own contributed no endpoint above, so nothing is requested for it.
        val mediaIdByPostId = mutableMapOf<Long, Long>()
        for ((endpointType, postIds) in byEndpoint) {
            mediaIdByPostId += rsSiteRestClient.fetchFeaturedMediaIds(site, endpointType, postIds)
        }
        if (mediaIdByPostId.isEmpty()) return@withContext emptyMap()

        // One media call for every image on the screen. Already chunked, retried and LRU-cached by
        // RsSiteRestClient, so revisiting the screen usually resolves without a request at all.
        val urlsByMediaId = rsSiteRestClient.fetchFeaturedImageUrls(
            site = site,
            mediaIds = mediaIdByPostId.values.distinct(),
            thumbnailDp = THUMBNAIL_DP,
            heroHeightDp = NO_HERO,
        )
        mediaIdByPostId.mapNotNull { (postId, mediaId) ->
            urlsByMediaId[mediaId]?.thumbnail?.let { postId to it }
        }.toMap()
    }

    /**
     * The REST endpoint a stats item type can be looked up on, or null when it has no featured
     * image to look up -- the homepage, an attachment, or a type we don't recognise.
     */
    private fun String?.toEndpointType(): PostEndpointType? = when (this?.lowercase()) {
        TYPE_POST -> PostEndpointType.Posts
        TYPE_PAGE -> PostEndpointType.Pages
        else -> null
    }

    companion object {
        private const val TYPE_POST = "post"
        private const val TYPE_PAGE = "page"

        /**
         * Matches `ThumbnailSize` in `StatsPostThumbnail`, so the server picks a render that fits
         * the box the rows draw. Keep the two in step if that one changes.
         */
        private const val THUMBNAIL_DP = 32

        /** These rows draw no hero, so its height is irrelevant. */
        private const val NO_HERO = 0
    }
}

/** A post the detail screen wants a thumbnail for. */
data class StatsThumbnailRequest(
    val postId: Long,
    val postType: String?
)
