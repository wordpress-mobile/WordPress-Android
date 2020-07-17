package org.wordpress.android.ui.reader.usecases

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_SAVED_FROM_OTHER_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_SAVED_FROM_SAVED_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_SAVED_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_LIST_VIEWED_FROM_POST_LIST_NOTICE
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

/**
 * This class handles bookmark/saveForLater button click events.
 * It updates the post in the database, tracks events, initiates pre-load content and shows snackbar/dialog.
 */
class ReaderPostBookmarkUseCase @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerPostActionsWrapper: ReaderPostActionsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper
) {
    private val _navigationEvents = MutableLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MutableLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents = _preloadPostEvents

    suspend fun toggleBookmark(blogId: Long, postId: Long, isBookmarkList: Boolean) {
        return withContext(bgDispatcher) {
            val bookmarked = updatePostInDb(blogId, postId)
            trackEvent(bookmarked, isBookmarkList)
            preloadContent(bookmarked, isBookmarkList, blogId, postId)

            val showSnackbarAction = prepareSnackbarAction()
            if (bookmarked && !isBookmarkList) {
                if (appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()) {
                    _navigationEvents.postValue(Event(ShowBookmarkedSavedOnlyLocallyDialog(
                            okButtonAction = {
                                appPrefsWrapper.setBookmarksSavedLocallyDialogShown()
                                showSnackbarAction.invoke()
                            })
                    ))
                } else {
                    showSnackbarAction.invoke()
                }
            }
        }
    }

    private fun preloadContent(
        bookmarked: Boolean,
        isBookmarkList: Boolean,
        blogId: Long,
        postId: Long
    ) {
        val cachePostContent = bookmarked && networkUtilsWrapper.isNetworkAvailable() && !isBookmarkList
        if (cachePostContent) {
            _preloadPostEvents.postValue(Event(PreLoadPostContent(blogId, postId)))
        }
    }

    private fun updatePostInDb(blogId: Long, postId: Long): Boolean {
        // TODO malinjir replace direct db access with access to repository.
        //  Also make sure PostUpdated event is emitted when we change the state of the post.
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

    private fun trackEvent(bookmarked: Boolean, isBookmarkList: Boolean) {
        val trackingEvent = when {
            bookmarked && isBookmarkList -> READER_POST_SAVED_FROM_SAVED_POST_LIST
            bookmarked && !isBookmarkList -> READER_POST_SAVED_FROM_OTHER_POST_LIST
            !bookmarked && isBookmarkList -> READER_POST_UNSAVED_FROM_SAVED_POST_LIST
            !bookmarked && !isBookmarkList -> READER_POST_UNSAVED_FROM_SAVED_POST_LIST
            else -> throw IllegalStateException("Developer error: This code should be unreachable.")
        }
        analyticsTrackerWrapper.track(trackingEvent)
    }

    private fun prepareSnackbarAction(): () -> Unit {
        return {
            _snackbarEvents.postValue(
                    Event(
                            SnackbarMessageHolder(
                                    string.reader_bookmark_snack_title,
                                    string.reader_bookmark_snack_btn,
                                    buttonAction = {
                                        analyticsTrackerWrapper
                                                .track(READER_SAVED_LIST_VIEWED_FROM_POST_LIST_NOTICE)
                                        _navigationEvents.postValue(Event(ShowBookmarkedTab))
                                    })
                    )
            )
        }
    }
}

data class PreLoadPostContent(val blogId: Long, val postId: Long)
