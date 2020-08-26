package org.wordpress.android.ui.reader.discover

import android.content.ActivityNotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_VISITED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.SHARED_ITEM_READER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.DEFAULT_SCOPE
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.SharePost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostDetail
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderComments
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowVideoViewer
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Started
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.usecases.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.ReaderPostBookmarkUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.AlreadyRunning
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.NoNetwork
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState.Failed.RequestFailed
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.widgets.AppRatingDialog.incrementInteractions
import javax.inject.Inject
import javax.inject.Named

// TODO malinjir start using this class in legacy ReaderPostAdapter and ReaderPostListFragment
class ReaderPostCardActionsHandler @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val reblogUseCase: ReblogUseCase,
    private val bookmarkUseCase: ReaderPostBookmarkUseCase,
    private val likeUseCase: PostLikeUseCase,
    private val siteNotificationsUseCase: ReaderSiteNotificationsUseCase,
    private val dispatcher: Dispatcher,
    @Named(DEFAULT_SCOPE) private val defaultScope: CoroutineScope
) {
    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MediatorLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents = _preloadPostEvents

    init {
        dispatcher.register(siteNotificationsUseCase)

        _navigationEvents.addSource(bookmarkUseCase.navigationEvents) { event ->
            _navigationEvents.value = event
        }

        _snackbarEvents.addSource(bookmarkUseCase.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _preloadPostEvents.addSource(bookmarkUseCase.preloadPostEvents) { event ->
            _preloadPostEvents.value = event
        }
    }

    fun onAction(post: ReaderPost, type: ReaderPostCardActionType, isBookmarkList: Boolean) {
        when (type) {
            FOLLOW -> handleFollowClicked(post)
            SITE_NOTIFICATIONS -> handleSiteNotificationsClicked(post.blogId)
            SHARE -> handleShareClicked(post)
            VISIT_SITE -> handleVisitSiteClicked(post)
            BLOCK_SITE -> handleBlockSiteClicked(post.postId, post.blogId)
            LIKE -> handleLikeClicked(post)
            BOOKMARK -> handleBookmarkClicked(post.postId, post.blogId, isBookmarkList)
            REBLOG -> handleReblogClicked(post)
            COMMENTS -> handleCommentsClicked(post.postId, post.blogId)
        }
    }

    fun handleOnItemClicked(post: ReaderPost) {
        incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST)

        if (post.isBookmarked) {
            analyticsTrackerWrapper.track(READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST)
        }
        _navigationEvents.postValue(Event(ShowPostDetail(post)))
    }

    fun handleVideoOverlayClicked(videoUrl: String) {
        _navigationEvents.postValue(Event(ShowVideoViewer(videoUrl)))
    }

    fun handleHeaderClicked(siteId: Long, feedId: Long) {
        _navigationEvents.postValue(Event(ShowBlogPreview(siteId, feedId)))
    }

    private fun handleFollowClicked(post: ReaderPost) {
        // todo: Annmarie add tracking dependent upon implementation (tracked in ReaderBlogActions)
        AppLog.d(AppLog.T.READER, "Follow not implemented")
    }

    // todo: Annmarie add tracking dependent upon implementation
    private fun handleSiteNotificationsClicked(blogId: Long) {
        defaultScope.launch {
            when (siteNotificationsUseCase.toggleNotification(blogId)) {
                is SiteNotificationState.Success, AlreadyRunning -> { // Do Nothing
                }
                is NoNetwork -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder((UiStringRes(R.string.error_network_connection))))
                    )
                }
                is RequestFailed -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder((UiStringRes(R.string.reader_error_request_failed_title))))
                    )
                }
            }
        }
    }

    private fun handleShareClicked(post: ReaderPost) {
        analyticsTrackerWrapper.track(SHARED_ITEM_READER, post.blogId)
        try {
            _navigationEvents.postValue(Event(SharePost(post)))
        } catch (ex: ActivityNotFoundException) {
            _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.reader_toast_err_share_intent))))
        }
    }

    private fun handleVisitSiteClicked(post: ReaderPost) {
        analyticsTrackerWrapper.track(READER_ARTICLE_VISITED)
        _navigationEvents.postValue(Event(OpenPost(post)))
    }

    private fun handleBlockSiteClicked(postId: Long, blogId: Long) {
        // todo: Annmarie add tracking dependent upon implementation
        AppLog.d(AppLog.T.READER, "Block site not implemented")
    }

    private fun handleLikeClicked(post: ReaderPost) {
        defaultScope.launch {
            when (likeUseCase.perform(post, !post.isLikedByCurrentUser)) {
                is Started, is ReaderRepositoryCommunication.Success, is SuccessWithData<*> -> {
                }
                is NetworkUnavailable -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
                    )
                }
                is RemoteRequestFailure -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.reader_error_request_failed_title)))
                    )
                }
            }
        }
    }

    private fun handleBookmarkClicked(postId: Long, blogId: Long, isBookmarkList: Boolean) {
        // todo: Annmarie add tracking dependent upon implementation
        defaultScope.launch {
            bookmarkUseCase.toggleBookmark(blogId, postId, isBookmarkList)
        }
    }

    private fun handleReblogClicked(post: ReaderPost) {
        val state = reblogUseCase.onReblogButtonClicked(post)
        val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
        if (navigationTarget != null) {
            _navigationEvents.postValue(Event(navigationTarget))
        } else {
            _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.reader_reblog_error))))
        }
    }

    private fun handleCommentsClicked(postId: Long, blogId: Long) {
        _navigationEvents.postValue(Event(ShowReaderComments(blogId, postId)))
    }

    fun onCleared() {
        dispatcher.unregister(siteNotificationsUseCase)
    }
}
