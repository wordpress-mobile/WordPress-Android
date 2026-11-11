package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.utils.ReaderUtils
import javax.inject.Inject
import javax.inject.Named

/**
 * Data access for the Reader post list. Reads run on the IO dispatcher, and post updates plus changes to
 * the followed tags and sites are exposed as a single [Flow].
 */
class ReaderPostListRepository @Inject constructor(
    private val readerPostUpdater: ReaderPostUpdater,
    private val tagsAndSitesUpdater: ReaderTagsAndSitesUpdater,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    val events: Flow<ReaderPostListEvent> = merge(
        readerPostUpdater.events,
        tagsAndSitesUpdater.events.map { event ->
            when (event) {
                is ReaderTagsAndSitesEvent.FollowedTagsChanged -> ReaderPostListEvent.FollowedTagsFetched

                is ReaderTagsAndSitesEvent.FollowedBlogsChanged ->
                    ReaderPostListEvent.FollowedBlogsFetched(event.didChange)
            }
        }
    )

    suspend fun getPosts(target: ReaderPostListTarget): ReaderPostList = withContext(ioDispatcher) {
        when (target) {
            is ReaderPostListTarget.Tag -> ReaderPostTable.getPostsWithTag(target.tag, MAX_ROWS, EXCLUDE_TEXT)
            is ReaderPostListTarget.Blog -> ReaderPostTable.getPostsInBlog(target.blogId, MAX_ROWS, EXCLUDE_TEXT)
            is ReaderPostListTarget.Feed -> ReaderPostTable.getPostsInFeed(target.feedId, MAX_ROWS, EXCLUDE_TEXT)
        }
    }

    suspend fun getPostCount(target: ReaderPostListTarget): Int = withContext(ioDispatcher) {
        when (target) {
            is ReaderPostListTarget.Tag -> ReaderPostTable.getNumPostsWithTag(target.tag)
            is ReaderPostListTarget.Blog -> ReaderPostTable.getNumPostsInBlog(target.blogId)
            is ReaderPostListTarget.Feed -> ReaderPostTable.getNumPostsInFeed(target.feedId)
        }
    }

    suspend fun getGapMarkerIds(tag: ReaderTag): ReaderBlogIdPostId? = withContext(ioDispatcher) {
        ReaderPostTable.getGapMarkerIdsForTag(tag)
    }

    suspend fun removeGapMarker(tag: ReaderTag) = withContext(ioDispatcher) {
        ReaderPostTable.removeGapMarkerForTag(tag)
    }

    suspend fun purgeUnbookmarkedPosts() = withContext(ioDispatcher) {
        ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag()
    }

    suspend fun deletePostsWithTag(tag: ReaderTag) = withContext(ioDispatcher) {
        ReaderPostTable.deletePostsWithTag(tag)
    }

    suspend fun shouldAutoUpdateTag(tag: ReaderTag): Boolean = withContext(ioDispatcher) {
        ReaderTagTable.shouldAutoUpdateTag(tag)
    }

    suspend fun tagExists(tag: ReaderTag): Boolean = withContext(ioDispatcher) {
        ReaderTagTable.tagExists(tag)
    }

    suspend fun isFollowedTag(tag: ReaderTag): Boolean = withContext(ioDispatcher) {
        ReaderTagTable.isFollowedTagName(tag.tagSlug)
    }

    suspend fun getFollowedTag(tagName: String): ReaderTag = withContext(ioDispatcher) {
        ReaderUtils.getTagFromTagName(tagName, ReaderTagType.FOLLOWED)
    }

    /**
     * Returns the tag with the passed endpoint, falling back to the default tag
     */
    suspend fun getTagFromEndpointOrDefault(endpoint: String): ReaderTag = withContext(ioDispatcher) {
        ReaderUtils.getTagFromEndpoint(endpoint) ?: ReaderUtils.getDefaultTag()
    }

    suspend fun hasFollowedBlogs(): Boolean = withContext(ioDispatcher) {
        ReaderBlogTable.hasFollowedBlogs()
    }

    suspend fun isFollowedSite(blog: ReaderBlog): Boolean = withContext(ioDispatcher) {
        (blog.hasFeedUrl() && ReaderBlogTable.isFollowedFeed(blog.feedId)) ||
                ReaderBlogTable.isFollowedBlog(blog.blogId)
    }

    /**
     * Returns the stored info for the blog or feed, or null if there isn't any
     */
    suspend fun getBlogInfo(blogId: Long, feedId: Long): ReaderBlog? = withContext(ioDispatcher) {
        readerBlogTableWrapper.getReaderBlog(blogId, feedId)
    }

    suspend fun isTimeToUpdateBlogInfo(blog: ReaderBlog): Boolean = withContext(ioDispatcher) {
        ReaderBlogTable.isTimeToUpdateBlogInfo(blog)
    }

    fun requestPosts(target: ReaderPostListTarget, action: UpdateAction) =
        readerPostUpdater.updatePosts(target, action)

    fun searchPosts(query: String, offset: Int) = readerPostUpdater.searchPosts(query, offset)

    /**
     * Updates followed tags and blogs, since following some sites (like P2s) changes both
     */
    fun updateTagsAndFollowedBlogs() = tagsAndSitesUpdater.update(setOf(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS))

    /**
     * Follows or unfollows the passed tag and returns whether it succeeded. The local write happens
     * right away, so callers can read the new follow state before this returns.
     */
    suspend fun followTag(tag: ReaderTag, follow: Boolean): Boolean =
        if (follow) tagsAndSitesUpdater.followTags(listOf(tag)) else tagsAndSitesUpdater.unfollowTag(tag)

    companion object {
        private const val MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY

        // the large "tbl_posts.text" column isn't shown in the list, so skip it when querying
        private const val EXCLUDE_TEXT = true
    }
}

sealed class ReaderPostListTarget {
    data class Tag(val tag: ReaderTag) : ReaderPostListTarget()
    data class Blog(val blogId: Long) : ReaderPostListTarget()
    data class Feed(val feedId: Long) : ReaderPostListTarget()
}

sealed class ReaderPostListEvent {
    data class UpdateStarted(val tag: ReaderTag?, val action: UpdateAction) : ReaderPostListEvent()
    data class UpdateEnded(
        val tag: ReaderTag?,
        val action: UpdateAction,
        val result: UpdateResult
    ) : ReaderPostListEvent()

    data class SearchStarted(val query: String, val offset: Int) : ReaderPostListEvent()
    data class SearchEnded(val query: String, val offset: Int, val didSucceed: Boolean) : ReaderPostListEvent()
    data object FollowedTagsFetched : ReaderPostListEvent()
    data class FollowedBlogsFetched(val didChange: Boolean) : ReaderPostListEvent()
}
