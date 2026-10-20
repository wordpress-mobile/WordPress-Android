package org.wordpress.android.ui.reader.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.actions.ReaderTagActions
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction
import org.wordpress.android.ui.reader.services.search.ReaderSearchServiceStarter
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.ContextProvider
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

/**
 * Data access for the Reader post list. Reads run on the IO dispatcher, and the EventBus events the
 * Reader services post are exposed as a single [Flow].
 */
class ReaderPostListRepository @Inject constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    val events: Flow<ReaderPostListEvent> = callbackFlow {
        val subscriber = EventSubscriber { event ->
            trySend(event).onFailure {
                AppLog.w(AppLog.T.READER, "reader post list > failed to deliver $event")
            }
        }
        eventBusWrapper.register(subscriber)
        awaitClose { eventBusWrapper.unregister(subscriber) }
    }

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

    fun requestPosts(target: ReaderPostListTarget, action: UpdateAction) {
        val context = contextProvider.getContext()
        when (target) {
            is ReaderPostListTarget.Tag -> ReaderPostServiceStarter.startServiceForTag(context, target.tag, action)
            is ReaderPostListTarget.Blog -> ReaderPostServiceStarter.startServiceForBlog(context, target.blogId, action)
            is ReaderPostListTarget.Feed -> ReaderPostServiceStarter.startServiceForFeed(context, target.feedId, action)
        }
    }

    fun searchPosts(query: String, offset: Int) {
        ReaderSearchServiceStarter.startService(contextProvider.getContext(), query, offset)
    }

    /**
     * Updates followed tags and blogs, since following some sites (like P2s) changes both
     */
    fun updateTagsAndFollowedBlogs() {
        ReaderUpdateServiceStarter.startService(
            contextProvider.getContext(),
            EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS)
        )
    }

    /**
     * Follows or unfollows the passed tag and returns whether it succeeded. The local write happens
     * right away, so callers can read the new follow state before this returns.
     */
    suspend fun followTag(tag: ReaderTag, follow: Boolean): Boolean = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val isLoggedIn = accountStore.hasAccessToken()
            val listener = ReaderActions.ActionListener { succeeded ->
                if (continuation.isActive) continuation.resume(succeeded)
            }
            val started = if (follow) {
                ReaderTagActions.addTag(tag, listener, isLoggedIn)
            } else {
                ReaderTagActions.deleteTag(tag, listener, isLoggedIn)
            }
            if (!started && continuation.isActive) continuation.resume(false)
        }
    }

    // EventBus invokes subscriber methods reflectively, so this class has to stay public
    class EventSubscriber(private val onEvent: (ReaderPostListEvent) -> Unit) {
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.UpdatePostsStarted) {
            onEvent(ReaderPostListEvent.UpdateStarted(event.readerTag, event.action))
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.UpdatePostsEnded) {
            onEvent(ReaderPostListEvent.UpdateEnded(event.readerTag, event.action, event.result))
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.SearchPostsStarted) {
            onEvent(ReaderPostListEvent.SearchStarted(event.query, event.offset))
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.SearchPostsEnded) {
            onEvent(ReaderPostListEvent.SearchEnded(event.query, event.offset, event.didSucceed()))
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.FollowedTagsFetched) {
            onEvent(ReaderPostListEvent.FollowedTagsFetched(event.didChange()))
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.FollowedBlogsFetched) {
            onEvent(ReaderPostListEvent.FollowedBlogsFetched(event.didChange()))
        }
    }

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
    data class FollowedTagsFetched(val didChange: Boolean) : ReaderPostListEvent()
    data class FollowedBlogsFetched(val didChange: Boolean) : ReaderPostListEvent()
}
