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

    fun isFollowedUrl(url: String) = ReaderBlogTable.isFollowedBlogUrl(url) || ReaderBlogTable.isFollowedFeedUrl(url)

    fun createFollowedTag(name: String): ReaderTag = ReaderUtils.createTagFromTagName(name, ReaderTagType.FOLLOWED)

    /**
     * Returns the normalized URL for the passed entry, adding a protocol if it has none, or null if
     * it isn't a valid network URL
     */
    fun normalizeEntryUrl(entry: String): String? {
        val url = UrlUtils.normalizeUrl(if (entry.contains("://")) entry else "http://$entry")
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
    suspend fun followUrl(url: String): Boolean = blogRepository.followFeedUrl(url, ReaderTracker.SOURCE_SETTINGS)

    /**
     * Returns null if the URL is reachable, otherwise the status code. The follow endpoint accepts any
     * URL, even one that doesn't exist, so this is checked first.
     */
    suspend fun checkUrlReachable(url: String): Int? = blogRepository.checkUrlReachable(url)
}

sealed class ReaderSubsEvent {
    data object FollowedTagsFetched : ReaderSubsEvent()
    data class FollowedBlogsFetched(val didChange: Boolean) : ReaderSubsEvent()
}
