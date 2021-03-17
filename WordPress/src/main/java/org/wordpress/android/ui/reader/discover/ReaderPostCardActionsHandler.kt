package org.wordpress.android.ui.reader.discover

import android.content.ActivityNotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_VISITED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_POST_REPORTED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_LIST_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST
import org.wordpress.android.analytics.AnalyticsTracker.Stat.SHARED_ITEM_READER
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction.DELETE
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction.NEW
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderRecommendedBlogsCardUiState.ReaderRecommendedBlogUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.SharePost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBlogPreview
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostDetail
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReaderComments
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportPost
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowVideoViewer
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REPORT_POST
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.TOGGLE_SEEN_STATUS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.usecases.BlockBlogUseCase
import org.wordpress.android.ui.reader.repository.usecases.BlockSiteState
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase.PostLikeState
import org.wordpress.android.ui.reader.repository.usecases.UndoBlockBlogUseCase
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.PreLoadPostContent
import org.wordpress.android.ui.reader.usecases.BookmarkPostState.Success
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchSiteUseCase.FetchSiteState
import org.wordpress.android.ui.reader.usecases.ReaderPostBookmarkUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.Error
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.PostSeenStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.UserNotAuthenticated
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_CARD
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_DETAILS
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSiteNotificationsUseCase.SiteNotificationState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.widgets.AppRatingDialogWrapper
import javax.inject.Inject
import javax.inject.Named

class ReaderPostCardActionsHandler @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val reblogUseCase: ReblogUseCase,
    private val bookmarkUseCase: ReaderPostBookmarkUseCase,
    private val followUseCase: ReaderSiteFollowUseCase,
    private val blockBlogUseCase: BlockBlogUseCase,
    private val likeUseCase: PostLikeUseCase,
    private val siteNotificationsUseCase: ReaderSiteNotificationsUseCase,
    private val undoBlockBlogUseCase: UndoBlockBlogUseCase,
    private val fetchSiteUseCase: ReaderFetchSiteUseCase,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val appRatingDialogWrapper: AppRatingDialogWrapper,
    private val seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private lateinit var coroutineScope: CoroutineScope

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MediatorLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents: LiveData<Event<PreLoadPostContent>> = _preloadPostEvents

    private val _followStatusUpdated = MediatorLiveData<FollowStatusChanged>()
    val followStatusUpdated: LiveData<FollowStatusChanged> = _followStatusUpdated

    // Used only in legacy ReaderPostListFragment and ReaderPostDetailFragment.
    // The discover tab observes reactive ReaderDiscoverDataProvider.
    private val _refreshPosts = MediatorLiveData<Event<Unit>>()
    val refreshPosts: LiveData<Event<Unit>> = _refreshPosts

    init {
        dispatcher.register(siteNotificationsUseCase)
    }

    fun initScope(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    suspend fun onAction(
        post: ReaderPost,
        type: ReaderPostCardActionType,
        isBookmarkList: Boolean,
        fromPostDetails: Boolean = false
    ) {
        withContext(bgDispatcher) {
            if (type == FOLLOW || type == SITE_NOTIFICATIONS) {
                val readerBlog = readerBlogTableWrapper.getReaderBlog(post.blogId, post.feedId)
                if (readerBlog == null) {
                    val isSiteFetched = preFetchSite(post)
                    if (!isSiteFetched) {
                        return@withContext
                    }
                }
            }
            handleAction(post, type, fromPostDetails, isBookmarkList)
        }
    }

    private suspend fun preFetchSite(post: ReaderPost): Boolean {
        var isSiteFetched = false
        when (fetchSiteUseCase.fetchSite(post.blogId, post.feedId, null)) {
            FetchSiteState.AlreadyRunning -> { // Do Nothing
            }
            FetchSiteState.Success -> {
                isSiteFetched = true
            }
            FetchSiteState.Failed.NoNetwork -> {
                _snackbarEvents.postValue(
                    Event(SnackbarMessageHolder((UiStringRes(R.string.error_network_connection))))
                )
            }
            FetchSiteState.Failed.RequestFailed -> {
                _snackbarEvents.postValue(
                    Event(
                        SnackbarMessageHolder((UiStringRes(R.string.reader_error_request_failed_title)))
                    )
                )
            }
        }
        return isSiteFetched
    }

    private suspend fun handleAction(
        post: ReaderPost,
        type: ReaderPostCardActionType,
        fromPostDetails: Boolean,
        isBookmarkList: Boolean
    ) {
        when (type) {
            FOLLOW -> handleFollowClicked(post)
            SITE_NOTIFICATIONS -> handleSiteNotificationsClicked(post.blogId)
            SHARE -> handleShareClicked(post)
            VISIT_SITE -> handleVisitSiteClicked(post)
            BLOCK_SITE -> handleBlockSiteClicked(post.blogId)
            LIKE -> handleLikeClicked(post, fromPostDetails)
            BOOKMARK -> handleBookmarkClicked(post.postId, post.blogId, isBookmarkList, fromPostDetails)
            REBLOG -> handleReblogClicked(post)
            COMMENTS -> handleCommentsClicked(post.postId, post.blogId)
            REPORT_POST -> handleReportPostClicked(post)
            TOGGLE_SEEN_STATUS -> handleToggleSeenStatusClicked(post, fromPostDetails)
        }
    }

    suspend fun handleOnItemClicked(post: ReaderPost) {
        withContext(bgDispatcher) {
            appRatingDialogWrapper.incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST)

            if (post.isBookmarked) {
                analyticsTrackerWrapper.track(READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST)
            }
            _navigationEvents.postValue(Event(ShowPostDetail(post)))
        }
    }

    suspend fun handleVideoOverlayClicked(videoUrl: String) {
        withContext(bgDispatcher) {
            _navigationEvents.postValue(Event(ShowVideoViewer(videoUrl)))
        }
    }

    suspend fun handleHeaderClicked(siteId: Long, feedId: Long, isFollowed: Boolean) {
        withContext(bgDispatcher) {
            _navigationEvents.postValue(Event(ShowBlogPreview(siteId, feedId, isFollowed)))
        }
    }

    suspend fun handleReportPostClicked(post: ReaderPost) {
        withContext(bgDispatcher) {
            val properties: MutableMap<String, Any> = HashMap()
            properties["blog_id"] = post.blogId
            properties["is_jetpack"] = post.isJetpack
            properties["post_id"] = post.postId
            analyticsTrackerWrapper.track(READER_POST_REPORTED, properties)
            _navigationEvents.postValue(Event(ShowReportPost(post.blogUrl)))
        }
    }

    private suspend fun handleToggleSeenStatusClicked(post: ReaderPost, fromPostDetails: Boolean) {
        val actionSource = if (fromPostDetails) {
            READER_POST_DETAILS
        } else {
            READER_POST_CARD
        }
        seenStatusToggleUseCase.toggleSeenStatus(post, actionSource).flowOn(bgDispatcher).collect { state ->
            when (state) {
                is Error -> {
                    state.message?.let {
                        _snackbarEvents.postValue(
                                Event(SnackbarMessageHolder(it))
                        )
                    }
                }
                is PostSeenStateChanged -> {
                    state.userMessage?.let {
                        _snackbarEvents.postValue(
                                Event(SnackbarMessageHolder(it))
                        )
                    }
                }
                is UserNotAuthenticated -> { // should not happen with current implementation
                    AppLog.e(
                            T.READER,
                            "User was not authenticated when attempting to toggle Seen/Unseen status of the post"
                    )
                }
            }
        }
    }

    suspend fun handleFollowRecommendedSiteClicked(recommendedBlogUiState: ReaderRecommendedBlogUiState) {
        val param = ReaderSiteFollowUseCase.Param(
                blogId = recommendedBlogUiState.blogId,
                blogName = recommendedBlogUiState.name,
                feedId = recommendedBlogUiState.feedId
        )
        followSite(param)
    }

    private suspend fun handleFollowClicked(post: ReaderPost) {
        followSite(ReaderSiteFollowUseCase.Param(post.blogId, post.feedId, post.blogName))
    }

    private suspend fun followSite(followSiteParam: ReaderSiteFollowUseCase.Param) {
        followUseCase.toggleFollow(followSiteParam).collect {
            when (it) {
                is FollowSiteState.Failed.NoNetwork -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder((UiStringRes(R.string.error_network_connection))))
                    )
                }
                is FollowSiteState.Failed.RequestFailed -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder((UiStringRes(R.string.reader_error_request_failed_title))))
                    )
                }
                is FollowSiteState.AlreadyRunning, FollowSiteState.Success -> Unit // Do nothing
                is FollowStatusChanged -> {
                    _followStatusUpdated.postValue(it)
                    siteNotificationsUseCase.fetchSubscriptions()

                    if (it.showEnableNotification) {
                        val action = prepareEnableNotificationSnackbarAction(followSiteParam.blogName, it.blogId)
                        action.invoke()
                    } else if (it.deleteNotificationSubscription) {
                        siteNotificationsUseCase.updateSubscription(it.blogId, DELETE)
                        siteNotificationsUseCase.updateNotificationEnabledForBlogInDb(it.blogId, false)
                    }
                }
            }
        }
    }

    private suspend fun handleSiteNotificationsClicked(blogId: Long) {
        when (siteNotificationsUseCase.toggleNotification(blogId)) {
            is SiteNotificationState.Success, SiteNotificationState.Failed.AlreadyRunning -> { // Do Nothing
            }
            is SiteNotificationState.Failed.NoNetwork -> {
                _snackbarEvents.postValue(
                        Event(SnackbarMessageHolder((UiStringRes(R.string.error_network_connection))))
                )
            }
            is SiteNotificationState.Failed.RequestFailed -> {
                _snackbarEvents.postValue(
                        Event(SnackbarMessageHolder((UiStringRes(R.string.reader_error_request_failed_title))))
                )
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

    private suspend fun handleBlockSiteClicked(blogId: Long) {
        blockBlogUseCase.blockBlog(blogId).collect {
            when (it) {
                is BlockSiteState.SiteBlockedInLocalDb -> {
                    _refreshPosts.postValue(Event(Unit))
                    _snackbarEvents.postValue(
                            Event(
                                    SnackbarMessageHolder(
                                            UiStringRes(R.string.reader_toast_blog_blocked),
                                            UiStringRes(R.string.undo),
                                            {
                                                coroutineScope.launch {
                                                    undoBlockBlogUseCase.undoBlockBlog(it.blockedBlogData)
                                                    _refreshPosts.postValue(Event(Unit))
                                                }
                                            })
                            )
                    )
                }
                BlockSiteState.Success, BlockSiteState.Failed.AlreadyRunning -> Unit // do nothing
                BlockSiteState.Failed.NoNetwork -> {
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.reader_toast_err_block_blog)))
                    )
                }
                BlockSiteState.Failed.RequestFailed -> {
                    _refreshPosts.postValue(Event(Unit))
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.reader_toast_err_block_blog)))
                    )
                }
            }
        }
    }

    private suspend fun handleLikeClicked(post: ReaderPost, fromPostDetails: Boolean) {
        likeUseCase.perform(post, !post.isLikedByCurrentUser, fromPostDetails).collect {
            when (it) {
                is PostLikeState.PostLikedInLocalDb -> {
                    _refreshPosts.postValue(Event(Unit))
                }
                is PostLikeState.Success, is PostLikeState.Unchanged, is PostLikeState.AlreadyRunning -> {
                }
                is PostLikeState.Failed.NoNetwork -> {
                    _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message))))
                }
                is PostLikeState.Failed.RequestFailed -> {
                    _refreshPosts.postValue(Event(Unit))
                    _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.reader_error_request_failed_title)))
                    )
                }
            }
        }
    }

    private suspend fun handleBookmarkClicked(
        postId: Long,
        blogId: Long,
        isBookmarkList: Boolean,
        fromPostDetails: Boolean
    ) {
        bookmarkUseCase.toggleBookmark(blogId, postId, isBookmarkList, fromPostDetails).collect {
            when (it) {
                is PreLoadPostContent -> _preloadPostEvents.postValue(Event(PreLoadPostContent(blogId, postId)))
                is Success -> {
                    // Content needs to be manually refreshed in the legacy ReaderPostListAdapter and
                    // ReaderPostDetailFragment
                    _refreshPosts.postValue(Event(Unit))

                    val showSnackbarAction = {
                        _snackbarEvents.postValue(
                                Event(
                                        SnackbarMessageHolder(
                                                UiStringRes(R.string.reader_bookmark_snack_title),
                                                UiStringRes(R.string.reader_bookmark_snack_btn),
                                                buttonAction = {
                                                    analyticsTrackerWrapper.track(
                                                            READER_SAVED_LIST_SHOWN,
                                                            mapOf("source" to "post_list_saved_post_notice")
                                                    )
                                                    _navigationEvents.postValue(Event(ShowBookmarkedTab))
                                                })
                                )
                        )
                    }
                    if (it.bookmarked && !isBookmarkList) {
                        if (appPrefsWrapper.shouldShowBookmarksSavedLocallyDialog()) {
                            _navigationEvents.postValue(
                                    Event(
                                            ShowBookmarkedSavedOnlyLocallyDialog(
                                                    okButtonAction = {
                                                        appPrefsWrapper.setBookmarksSavedLocallyDialogShown()
                                                        showSnackbarAction.invoke()
                                                    })
                                    )
                            )
                        } else {
                            showSnackbarAction.invoke()
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleReblogClicked(post: ReaderPost) {
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

    private fun prepareEnableNotificationSnackbarAction(blogName: String?, blogId: Long): () -> Unit {
        return {
            val thisSite = resourceProvider.getString(R.string.reader_followed_blog_notifications_this)
            val blog = if (blogName?.isEmpty() == true) thisSite else blogName
            val notificationMessage = htmlMessageUtils
                    .getHtmlMessageFromStringFormatResId(
                            R.string.reader_followed_blog_notifications,
                            "<b>",
                            blog,
                            "</b>"
                    )
            _snackbarEvents.postValue(
                    Event(
                            SnackbarMessageHolder(
                                    UiStringText(notificationMessage),
                                    UiStringRes(R.string.reader_followed_blog_notifications_action),
                                    buttonAction = {
                                        coroutineScope.launch(bgDispatcher) {
                                            analyticsTrackerWrapper
                                                    .track(FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED, blogId)
                                            siteNotificationsUseCase.updateSubscription(blogId, NEW)
                                            siteNotificationsUseCase.updateNotificationEnabledForBlogInDb(blogId, true)
                                        }
                                    }
                            )
                    )
            )
        }
    }

    fun onCleared() {
        dispatcher.unregister(siteNotificationsUseCase)
    }
}
