package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_SAVED_FROM_OTHER_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_SAVED_FROM_SAVED_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_SAVED_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_SAVED_FROM_DETAILS
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_DETAILS
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

/**
 * This class handles bookmark/saveForLater button click events.
 * It updates the post in the database, tracks events, initiates pre-load content and shows snackbar/dialog.
 */
class ReaderPostBookmarkUseCase @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    suspend fun toggleBookmark(
        blogId: Long,
        postId: Long,
        isBookmarkList: Boolean,
        fromPostDetails: Boolean = false
    ) = flow<BookmarkPostState> {
        val bookmarked = updatePostInDb(blogId, postId)
        trackEvent(bookmarked, isBookmarkList, fromPostDetails)
        preloadPostContentIfNecessary(bookmarked, isBookmarkList, blogId, postId)
        emit(Success(bookmarked))
    }

    private suspend fun FlowCollector<BookmarkPostState>.preloadPostContentIfNecessary(
        bookmarked: Boolean,
        isBookmarkList: Boolean,
        blogId: Long,
        postId: Long
    ) {
        val cachePostContent = bookmarked && networkUtilsWrapper.isNetworkAvailable() && !isBookmarkList
        if (cachePostContent) {
            emit(PreLoadPostContent(blogId, postId))
        }
    }

    private fun updatePostInDb(blogId: Long, postId: Long): Boolean {
        val post = readerPostTableWrapper.getBlogPost(blogId, postId, true)
                ?: throw IllegalStateException("Post displayed on the UI not found in DB.")

        val setToBookmarked = !post.isBookmarked

        if (setToBookmarked) {
            readerPostActionsWrapper.addToBookmarked(post)
        } else {
            readerPostActionsWrapper.removeFromBookmarked(post)
        }
        return setToBookmarked
    }

    private fun trackEvent(bookmarked: Boolean, isBookmarkList: Boolean, fromPostDetails: Boolean) {
        val trackingEvent = when {
            !fromPostDetails && bookmarked && isBookmarkList -> READER_POST_SAVED_FROM_SAVED_POST_LIST
            !fromPostDetails && bookmarked && !isBookmarkList -> READER_POST_SAVED_FROM_OTHER_POST_LIST
            !fromPostDetails && !bookmarked && isBookmarkList -> READER_POST_UNSAVED_FROM_SAVED_POST_LIST
            !fromPostDetails && !bookmarked && !isBookmarkList -> READER_POST_SAVED_FROM_OTHER_POST_LIST
            fromPostDetails && bookmarked && !isBookmarkList -> READER_POST_SAVED_FROM_DETAILS
            fromPostDetails && !bookmarked && !isBookmarkList -> READER_POST_UNSAVED_FROM_DETAILS
            else -> throw IllegalStateException("Developer error: This code should be unreachable.")
        }
        analyticsTrackerWrapper.track(trackingEvent)
    }
}

sealed class BookmarkPostState {
    data class PreLoadPostContent(val blogId: Long, val postId: Long) : BookmarkPostState()
    data class Success(val bookmarked: Boolean) : BookmarkPostState()
}
