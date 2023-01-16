package org.wordpress.android.ui.engagement

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.R.string
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.engagement.EngageItem.LikedItem
import org.wordpress.android.ui.engagement.EngageItem.NextLikesPageLoader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewCommentInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewPostInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteById
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteByUrl
import org.wordpress.android.ui.engagement.EngagedListServiceRequestEvent.RequestBlogPost
import org.wordpress.android.ui.engagement.EngagedListServiceRequestEvent.RequestComment
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_NOTIFICATION_LIST
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_READER_LIST
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Loading
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.GetLikesUseCase.PagingInfo
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_COMMENT_LIKES
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_POST_LIKES
import org.wordpress.android.ui.engagement.PreviewBlogByUrlSource.LIKED_COMMENT_USER_HEADER
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.COMMENT_LIKE_NOTIFICATION
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class EngagedPeopleListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val getLikesHandler: GetLikesHandler,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val engagementUtils: EngagementUtils,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private var getLikesJob: Job? = null

    private var listScenario: ListScenario? = null

    private val _onSnackbarMessage = MediatorLiveData<Event<SnackbarMessageHolder>>()
    private val _updateLikesState = MediatorLiveData<GetLikesState>()
    private val _onNavigationEvent = MutableLiveData<Event<EngagedListNavigationEvent>>()
    private val _onServiceRequestEvent = MutableLiveData<Event<EngagedListServiceRequestEvent>>()

    val onSnackbarMessage: LiveData<Event<SnackbarMessageHolder>> = _onSnackbarMessage
    val uiState: LiveData<EngagedPeopleListUiState> = _updateLikesState.map { state ->
        buildUiState(state, listScenario)
    }
    val onNavigationEvent: LiveData<Event<EngagedListNavigationEvent>> = _onNavigationEvent
    val onServiceRequestEvent: LiveData<Event<EngagedListServiceRequestEvent>> = _onServiceRequestEvent

    data class EngagedPeopleListUiState(
        val numLikes: Int = 0,
        val showLoading: Boolean,
        val engageItemsList: List<EngageItem>,
        val showEmptyState: Boolean,
        val emptyStateTitle: UiString? = null,
        val emptyStateAction: (() -> Unit)? = null,
        val emptyStateButtonText: UiString? = null
    )

    fun start(listScenario: ListScenario) {
        if (isStarted) return
        isStarted = true

        this.listScenario = listScenario

        _onSnackbarMessage.addSource(getLikesHandler.snackbarEvents) { event ->
            _onSnackbarMessage.value = event
        }

        _updateLikesState.addSource(getLikesHandler.likesStatusUpdate) { state ->
            _updateLikesState.value = state
        }

        onRefreshData()
    }

    private fun onRefreshData() {
        loadRequest(listScenario, requestPostOrComment = true, requestNextPage = false)
    }

    private fun requestPostOrCommentIfNeeded(
        listScenarioType: ListScenarioType,
        siteId: Long,
        postOrCommentId: Long,
        commentPostId: Long
    ) {
        val postId = if (listScenarioType == LOAD_POST_LIKES) postOrCommentId else commentPostId
        val commentId = if (listScenarioType == LOAD_COMMENT_LIKES) postOrCommentId else 0L

        if (!readerUtilsWrapper.postExists(
                siteId,
                postId
            )
        ) {
            _onServiceRequestEvent.value = Event(RequestBlogPost(siteId, postId))
        }

        if (listScenarioType == LOAD_COMMENT_LIKES && !readerUtilsWrapper.commentExists(
                siteId,
                postId,
                commentId
            )
        ) {
            _onServiceRequestEvent.value = Event(RequestComment(siteId, postId, commentId))
        }
    }

    @Suppress("ForbiddenComment")
    private fun loadRequest(
        listScenario: ListScenario?,
        requestPostOrComment: Boolean,
        requestNextPage: Boolean
    ) {
        if (listScenario == null) return

        if (requestPostOrComment) {
            requestPostOrCommentIfNeeded(
                listScenario.type,
                listScenario.siteId,
                listScenario.postOrCommentId,
                listScenario.commentPostId
            )
        }

        getLikesJob?.cancel()
        getLikesJob = launch(bgDispatcher) {
            // TODO: currently API is not sorting the likes as the list in notifications does,
            // use case logic has code to sort based on a list of ids (ideally the available likers ids taken
            // from the notification).
            // Keeping the logic for now, but remove empty listOf and relevant logic when API will sort likes
            when (listScenario.type) {
                LOAD_POST_LIKES -> getLikesHandler.handleGetLikesForPost(
                    LikeGroupFingerPrint(
                        listScenario.siteId,
                        listScenario.postOrCommentId,
                        listScenario.headerData.numLikes
                    ),
                    requestNextPage
                )
                LOAD_COMMENT_LIKES -> getLikesHandler.handleGetLikesForComment(
                    LikeGroupFingerPrint(
                        listScenario.siteId,
                        listScenario.postOrCommentId,
                        listScenario.headerData.numLikes
                    ),
                    requestNextPage
                )
            }
        }
    }

    private fun buildUiState(updateLikesState: GetLikesState?, listScenario: ListScenario?): EngagedPeopleListUiState {
        val likedItem = listScenario?.headerData?.let {
            listOf(
                LikedItem(
                    author = it.authorName,
                    postOrCommentText = it.snippetText,
                    authorAvatarUrl = it.authorAvatarUrl,
                    likedItemId = listScenario.postOrCommentId,
                    likedItemSiteId = listScenario.siteId,
                    likedItemSiteUrl = listScenario.commentSiteUrl,
                    likedItemPostId = listScenario.commentPostId,
                    authorUserId = it.authorUserId,
                    authorPreferredSiteId = it.authorPreferredSiteId,
                    authorPreferredSiteUrl = it.authorPreferredSiteUrl,
                    onGravatarClick = ::onSiteLinkHolderClicked,
                    blogPreviewSource = when (listScenario.source) {
                        LIKE_NOTIFICATION_LIST -> ReaderTracker.SOURCE_NOTIFICATION
                        LIKE_READER_LIST -> ReaderTracker.SOURCE_READER_LIKE_LIST
                    },
                    onHeaderClicked = ::onHeaderClicked
                )
            )
        } ?: listOf()

        val likers = when (updateLikesState) {
            is LikesData -> {
                engagementUtils.likesToEngagedPeople(
                    updateLikesState.likes,
                    ::onUserProfileHolderClicked,
                    listScenario?.source
                ) + appendNextPageLoaderIfNeeded(updateLikesState.hasMore, true, updateLikesState.pageInfo)
            }
            is Failure -> {
                engagementUtils.likesToEngagedPeople(
                    updateLikesState.cachedLikes,
                    ::onUserProfileHolderClicked,
                    listScenario?.source
                ) + appendNextPageLoaderIfNeeded(updateLikesState.hasMore, false, updateLikesState.pageInfo)
            }
            Loading, null -> listOf()
        }

        var showEmptyState = false
        var emptyStateTitle: UiString? = null
        var emptyStateAction: (() -> Unit)? = null

        if (updateLikesState is Failure) {
            updateLikesState.emptyStateData.let {
                showEmptyState = it.showEmptyState
                emptyStateTitle = it.title
                emptyStateAction = ::onRefreshData
            }
        }

        return EngagedPeopleListUiState(
            showLoading = updateLikesState is Loading,
            engageItemsList = likedItem + likers,
            showEmptyState = showEmptyState,
            emptyStateTitle = emptyStateTitle,
            emptyStateAction = emptyStateAction,
            emptyStateButtonText = emptyStateAction?.let { UiStringRes(string.retry) }
        )
    }

    private fun appendNextPageLoaderIfNeeded(
        hasMore: Boolean,
        isLoading: Boolean,
        pageInfo: PagingInfo
    ): List<EngageItem> {
        return if (hasMore) {
            listOf(NextLikesPageLoader(isLoading) {
                loadRequest(listScenario, requestPostOrComment = false, requestNextPage = true)
                analyticsUtilsWrapper.trackLikeListFetchedMore(
                    EngagementNavigationSource.getSourceDescription(listScenario?.source),
                    ListScenarioType.getSourceDescription(listScenario?.type),
                    pageInfo.page + 1,
                    pageInfo.pageLength
                )
            })
        } else {
            listOf()
        }
    }

    private fun onUserProfileHolderClicked(userProfile: UserProfile, source: EngagementNavigationSource?) {
        _onNavigationEvent.value = Event(
            OpenUserProfileBottomSheet(
                userProfile,
                ::onSiteLinkHolderClicked,
                source
            )
        )
    }

    private fun onSiteLinkHolderClicked(siteId: Long, siteUrl: String, source: String) {
        if (ReaderTracker.isUserProfileSource(source)) {
            analyticsUtilsWrapper.trackUserProfileSiteShown()
        }

        if (siteId <= 0L && siteUrl.isNotEmpty()) {
            _onNavigationEvent.value = Event(PreviewSiteByUrl(siteUrl, source))
        } else if (siteId > 0L) {
            _onNavigationEvent.value = Event(PreviewSiteById(siteId, source))
        }
    }

    private fun onHeaderClicked(siteId: Long, siteUrl: String, postOrCommentId: Long, commentPostId: Long) {
        _onNavigationEvent.value = Event(
            if (commentPostId > 0) {
                if (readerUtilsWrapper.postAndCommentExists(siteId, commentPostId, postOrCommentId)) {
                    PreviewCommentInReader(siteId, commentPostId, postOrCommentId, COMMENT_LIKE_NOTIFICATION)
                } else {
                    PreviewSiteByUrl(siteUrl, LIKED_COMMENT_USER_HEADER.sourceDescription)
                }
            } else {
                PreviewPostInReader(siteId, postOrCommentId)
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public override fun onCleared() {
        super.onCleared()
        getLikesJob?.cancel()
        getLikesHandler.clear()
    }
}
