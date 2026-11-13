package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.saved.ReaderSavedPostsRepository
import org.wordpress.android.ui.reader.saved.toSaveIdentity
import org.wordpress.android.ui.reader.saved.toSavedPostDetails
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Failed
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.NoNetwork
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

/**
 * This class handles bookmark/saveForLater button click events.
 * It updates the post in the database, tracks events, initiates pre-load content and shows snackbar/dialog.
 */
class ReaderPostBookmarkUseCase @Inject constructor(
    private val readerTracker: ReaderTracker,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerPostRepository: ReaderPostRepository,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val savedPostsRepository: ReaderSavedPostsRepository
) {
    suspend fun toggleBookmark(
        post: ReaderPost,
        isBookmarkList: Boolean,
        source: String
    ) = flow {
        val bookmarked = if (savedPostsRepository.isEnabled) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(NoNetwork)
                return@flow
            }
            updatePostOnAccount(post.blogId, post.postId) ?: run {
                emit(Failed)
                return@flow
            }
        } else {
            updatePostInDb(post.blogId, post.postId)
        }
        trackEvent(bookmarked, isBookmarkList, post, source)
        preloadPostContentIfNecessary(bookmarked, isBookmarkList, post.blogId, post.postId)
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

    /**
     * Saves or unsaves the post on the account, then keeps the local copy in step so it can still be
     * read offline. Returns whether the post is now saved, or null if the account couldn't be updated.
     */
    private suspend fun updatePostOnAccount(blogId: Long, postId: Long): Boolean? {
        val post = getPostFromDb(blogId, postId)
        val setToBookmarked = !savedPostsRepository.isSaved(post)
        val details = if (setToBookmarked) post.toSavedPostDetails() else null
        if (!savedPostsRepository.setSaved(post.toSaveIdentity(), setToBookmarked, details)) {
            return null
        }
        // the local flag can disagree with the account, so only change it when it differs
        if (post.isBookmarked != setToBookmarked) {
            readerPostRepository.setBookmarked(post, setToBookmarked)
        }
        return setToBookmarked
    }

    @Suppress("UseCheckOrError")
    private fun getPostFromDb(blogId: Long, postId: Long): ReaderPost =
        readerPostTableWrapper.getBlogPost(blogId, postId, true)
            ?: throw IllegalStateException("Post displayed on the UI not found in DB.")

    private suspend fun updatePostInDb(blogId: Long, postId: Long): Boolean {
        val post = getPostFromDb(blogId, postId)
        val setToBookmarked = !post.isBookmarked
        readerPostRepository.setBookmarked(post, setToBookmarked)
        return setToBookmarked
    }

    @Suppress("ComplexMethod", "UseCheckOrError")
    private fun trackEvent(
        bookmarked: Boolean,
        isBookmarkList: Boolean,
        post: ReaderPost,
        source: String
    ) {
        val fromPostDetails = source == ReaderTracker.SOURCE_POST_DETAIL
        val trackingEvent = when {
            !fromPostDetails && bookmarked && isBookmarkList ->
                AnalyticsTracker.Stat.READER_POST_SAVED_FROM_SAVED_POST_LIST
            !fromPostDetails && bookmarked && !isBookmarkList ->
                AnalyticsTracker.Stat.READER_POST_SAVED_FROM_OTHER_POST_LIST
            !fromPostDetails && !bookmarked && isBookmarkList ->
                AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_SAVED_POST_LIST
            !fromPostDetails && !bookmarked && !isBookmarkList ->
                AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_OTHER_POST_LIST
            fromPostDetails && bookmarked && !isBookmarkList ->
                AnalyticsTracker.Stat.READER_POST_SAVED_FROM_DETAILS
            fromPostDetails && !bookmarked && !isBookmarkList ->
                AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_DETAILS
            else -> throw IllegalStateException("Developer error: This code should be unreachable.")
        }
        readerTracker.trackBlog(trackingEvent, post.blogId, post.feedId, post.isFollowedByCurrentUser, source)
    }
}

sealed class BookmarkPostState {
    data class PreLoadPostContent(val blogId: Long, val postId: Long) : BookmarkPostState()
    data class Success(val bookmarked: Boolean) : BookmarkPostState()
    data object NoNetwork : BookmarkPostState()
    data object Failed : BookmarkPostState()
}
