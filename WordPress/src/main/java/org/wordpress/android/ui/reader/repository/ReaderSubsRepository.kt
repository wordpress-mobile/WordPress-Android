package org.wordpress.android.ui.reader.repository

import android.webkit.URLUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.UrlUtils
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * Data access for Manage subscriptions: the followed tags and sites, and following them by name or URL.
 * Changes to the followed tags and sites are exposed as a [Flow].
 */
class ReaderSubsRepository @Inject constructor(
    private val tagsAndSitesUpdater: ReaderTagsAndSitesUpdater,
    private val blogRepository: ReaderBlogRepository,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    val events: Flow<ReaderSubsEvent> = tagsAndSitesUpdater.events.map { event ->
        when (event) {
            is ReaderTagsAndSitesEvent.FollowedTagsChanged -> ReaderSubsEvent.FollowedTagsFetched
            is ReaderTagsAndSitesEvent.FollowedBlogsChanged -> ReaderSubsEvent.FollowedBlogsFetched(event.didChange)
        }
    }

    suspend fun getFollowedTags(): List<ReaderTag> = withContext(ioDispatcher) {
        ReaderTagTable.getFollowedTags()
    }

    suspend fun getFollowedBlogs(): List<ReaderBlog> = withContext(ioDispatcher) {
        ReaderBlogTable.getFollowedBlogs()
    }

    fun isValidTagName(name: String) = ReaderTag.isValidTagName(name)

    fun isFollowedTagName(name: String): Boolean = ReaderTagTable.isFollowedTagName(name)

    /**
     * Returns true if the passed URL matches a followed site's URL or feed URL by [urlKey], since the stored
     * URLs are spelled however the server returned them
     */
    suspend fun isFollowedUrl(url: String): Boolean {
        val key = urlKey(url) ?: return false
        return withContext(ioDispatcher) {
            ReaderBlogTable.getFollowedBlogs().any { urlKey(it.url) == key || urlKey(it.feedUrl) == key }
        }
    }

    fun createFollowedTag(name: String): ReaderTag = ReaderUtils.createTagFromTagName(name, ReaderTagType.FOLLOWED)

    /**
     * Returns the normalized URL for the passed entry, adding a protocol if it has none, or null if
     * it isn't a valid network URL
     */
    fun normalizeEntryUrl(entry: String): String? {
        val url = UrlUtils.normalizeUrl(withScheme(entry))
        return url.takeIf { URLUtil.isNetworkUrl(it) }
    }

    fun getHost(url: String): String = UrlUtils.getHost(url)

    fun requestUpdate(tasks: Set<UpdateTask>) = tagsAndSitesUpdater.update(tasks)

    suspend fun followTag(tag: ReaderTag, follow: Boolean): Boolean =
        if (follow) tagsAndSitesUpdater.followTags(listOf(tag)) else tagsAndSitesUpdater.unfollowTag(tag)

    suspend fun followSite(blogId: Long, feedId: Long, follow: Boolean): Boolean =
        if (feedId != 0L) {
            blogRepository.followFeed(feedId, follow, ReaderTracker.SOURCE_SETTINGS)
        } else {
            blogRepository.followBlog(blogId, feedId, follow, ReaderTracker.SOURCE_SETTINGS)
        }

    /**
     * Follows the passed URL as a feed, since typed URLs are more likely to point to a feed than a
     * WordPress site (and the endpoint follows it as a site if it is one)
     */
    suspend fun followUrl(url: String, blogInfo: ReaderBlog?): Boolean =
        blogRepository.followFeedUrl(url, blogInfo, ReaderTracker.SOURCE_SETTINGS)

    /**
     * Returns the site or feed for the passed URL, saving it locally, or null if it can't be found
     */
    suspend fun lookupUrl(url: String): ReaderBlog? = blogRepository.lookupFeedUrl(url)

    /**
     * Returns null if the URL is reachable, otherwise the status code. The follow endpoint accepts any
     * URL, even one that doesn't exist, so this is checked first.
     */
    suspend fun checkUrlReachable(url: String): Int? = blogRepository.checkUrlReachable(url)

    companion object {
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443

        /**
         * Returns the URL without its scheme, "www.", default port, trailing slash or fragment, or null if
         * it has no host. The query and the path's case are kept, since they can tell feeds apart.
         */
        internal fun urlKey(url: String): String? {
            val uri = runCatching { URI(withScheme(url).trim()) }.getOrNull()
            val host = uri?.host?.lowercase(Locale.ROOT)?.removePrefix("www.")?.takeIf { it.isNotEmpty() }
            if (uri == null || host == null) return null
            val port = uri.port.takeIf { it != -1 && it != HTTP_PORT && it != HTTPS_PORT }?.let { ":$it" }.orEmpty()
            val query = uri.rawQuery?.let { "?$it" }.orEmpty()
            return host + port + uri.rawPath.orEmpty().trimEnd('/') + query
        }

        private fun withScheme(url: String) = if (url.contains("://")) url else "http://$url"
    }
}

sealed class ReaderSubsEvent {
    data object FollowedTagsFetched : ReaderSubsEvent()
    data class FollowedBlogsFetched(val didChange: Boolean) : ReaderSubsEvent()
}
