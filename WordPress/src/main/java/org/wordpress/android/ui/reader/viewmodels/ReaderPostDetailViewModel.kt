package org.wordpress.android.ui.reader.viewmodels

import androidx.annotation.AttrRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.wrappers.ReaderCommentTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderCommentList
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.TrailingLabelTextItem
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameString
import org.wordpress.android.ui.engagement.EngagementUtils
import org.wordpress.android.ui.engagement.GetLikesHandler
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Loading
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.HeaderData
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderConstants.READER_COMMENTS_TO_REQUEST_FOR_POST_SNIPPET
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsEnded
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsScenario.COMMENT_SNIPPET
import org.wordpress.android.ui.reader.ReaderEvents.UpdateCommentsStarted
import org.wordpress.android.ui.reader.ReaderPostDetailUiStateBuilder
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.CHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ReplaceRelatedPostDetailsWithHistory
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowEngagedPeopleList
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostInWebView
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowRelatedPostDetails
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.discover.ReaderPostActions
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.models.ReaderSimplePostList
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.services.comment.wrapper.ReaderCommentServiceStarterWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState
import org.wordpress.android.ui.reader.usecases.ReaderGetPostUseCase
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.CommentSnippetState.CommentSnippetData
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.uistates.CommentSnippetItemState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiDimen
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.WpUrlUtilsWrapper
import org.wordpress.android.util.config.CommentsSnippetFeatureConfig
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@Suppress("LargeClass")
@HiltViewModel
class ReaderPostDetailViewModel @Inject constructor(
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder,
    private val postDetailUiStateBuilder: ReaderPostDetailUiStateBuilder,
    private val reblogUseCase: ReblogUseCase,
    private val readerFetchRelatedPostsUseCase: ReaderFetchRelatedPostsUseCase,
    private val readerGetPostUseCase: ReaderGetPostUseCase,
    private val readerFetchPostUseCase: ReaderFetchPostUseCase,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val readerTracker: ReaderTracker,
    private val eventBusWrapper: EventBusWrapper,
    private val wpUrlUtilsWrapper: WpUrlUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val getLikesHandler: GetLikesHandler,
    private val likesEnhancementsFeatureConfig: LikesEnhancementsFeatureConfig,
    private val engagementUtils: EngagementUtils,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val contextProvider: ContextProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val commentsSnippetFeatureConfig: CommentsSnippetFeatureConfig,
    private val readerCommentTableWrapper: ReaderCommentTableWrapper,
    private val readerCommentServiceStarterWrapper: ReaderCommentServiceStarterWrapper
) : ScopedViewModel(mainDispatcher) {
    private var getLikesJob: Job? = null

    private val _uiState = MediatorLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _refreshPost = MediatorLiveData<Event<Unit>>()
    val refreshPost: LiveData<Event<Unit>> = _refreshPost

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _updateLikesState = MediatorLiveData<GetLikesState>()
    val likesUiState: LiveData<TrainOfFacesUiState> = _updateLikesState.map { state ->
        buildLikersUiState(state)
    }

    private val _commentSnippetState = MutableLiveData<CommentSnippetState>()
    val commentSnippetState: LiveData<CommentSnippetUiState> = _commentSnippetState.map { state ->
        postDetailUiStateBuilder.buildCommentSnippetUiState(state, post, ::onCommentSnippetClicked)
    }

    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    /**
     * Post which is about to be reblogged after the user selects a target site.
     */
    private var pendingReblogPost: ReaderPost? = null

    private var isStarted = false
    private var isRelatedPost: Boolean = false

    var isFeed: Boolean = false
    var interceptedUri: String? = null

    var post: ReaderPost? = null
    val hasPost: Boolean
        get() = post != null

    private data class RenderedLikesData(val blogId: Long, val postId: Long, val numLikes: Int, val iLike: Boolean) {
        fun isMatchingPostLikeStatus(post: ReaderPost): Boolean {
            return blogId != post.blogId || postId != post.postId ||
                    numLikes != post.numLikes || iLike != post.isLikedByCurrentUser
        }
    }

    private data class RenderedRepliesData(val blogId: Long?, val postId: Long?, val numReplies: Int?) {
        fun isMatchingPostCommentsStatus(blogId: Long, postId: Long, numReplies: Int): Boolean {
            return blogId != this.blogId || postId != this.postId || numReplies != this.numReplies
        }
    }

    private var lastRenderedLikesData: RenderedLikesData? = null
    private var lastRenderedRepliesData: RenderedRepliesData? = null

    private val shouldOfferSignIn: Boolean
        get() = wpUrlUtilsWrapper.isWordPressCom(interceptedUri) && !accountStore.hasAccessToken()

    data class TrainOfFacesUiState(
        val showLikeFacesTrainContainer: Boolean,
        val showLoading: Boolean,
        val engageItemsList: List<TrainOfAvatarsItem>,
        val showEmptyState: Boolean,
        val emptyStateTitle: UiString? = null,
        val contentDescription: UiString,
        val goingToShowFaces: Boolean
    )

    sealed class CommentSnippetState {
        object Loading : CommentSnippetState()
        data class Empty(
            val message: UiString
        ) : CommentSnippetState()
        data class Failure(
            val message: UiString
        ) : CommentSnippetState()
        data class CommentSnippetData(
            val comments: ReaderCommentList
        ) : CommentSnippetState()
    }

    data class CommentSnippetUiState(
        val commentsNumber: Int,
        val showFollowConversation: Boolean,
        val snippetItems: List<CommentSnippetItemState>
    )

    init {
        eventBusWrapper.register(readerFetchRelatedPostsUseCase)
        if (commentsSnippetFeatureConfig.isEnabled()) {
            eventBusWrapper.register(this)
        }
    }

    fun start(isRelatedPost: Boolean, isFeed: Boolean, interceptedUri: String?) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.isRelatedPost = isRelatedPost
        this.isFeed = isFeed
        this.interceptedUri = interceptedUri

        init()
    }

    private fun init() {
        readerPostCardActionsHandler.initScope(viewModelScope)
        _uiState.addSource(readerPostCardActionsHandler.followStatusUpdated) { data ->
            val currentUiState: ReaderPostDetailsUiState? = (_uiState.value as? ReaderPostDetailsUiState)

            currentUiState?.let {
                findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                    post.isFollowedByCurrentUser = data.following
                    updateFollowButtonUiState(
                            currentUiState = currentUiState,
                            isFollowed = post.isFollowedByCurrentUser
                    )
                }
            }
        }

        _refreshPost.addSource(readerPostCardActionsHandler.refreshPosts) {
            val currentUiState: ReaderPostDetailsUiState? = (_uiState.value as? ReaderPostDetailsUiState)
            currentUiState?.let {
                findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                    if (likesEnhancementsFeatureConfig.isEnabled()) {
                        onRefreshLikersData(
                                post,
                                true
                        )
                    }
                    if (commentsSnippetFeatureConfig.isEnabled()) {
                        onRefreshCommentsData(post.blogId, post.postId)
                    }
                    updatePostActions(post)
                }
            }
        }

        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        if (likesEnhancementsFeatureConfig.isEnabled()) {
            _snackbarEvents.addSource(getLikesHandler.snackbarEvents) { event ->
                _snackbarEvents.value = event
            }
        }

        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            val target = event.peekContent()
            if (target is ShowSitePickerForResult) {
                pendingReblogPost = target.post
            }
            _navigationEvents.value = event
        }

        if (likesEnhancementsFeatureConfig.isEnabled()) {
            _updateLikesState.addSource(getLikesHandler.likesStatusUpdate) { state ->
                _updateLikesState.value = state
            }
        }
    }

    fun showJetpackPoweredBottomSheet() {
        _showJetpackPoweredBottomSheet.value = Event(true)
    }

    fun onRefreshCommentsData(blogId: Long, postId: Long) {
        if (!commentsSnippetFeatureConfig.isEnabled()) return

        val post = readerPostTableWrapper.getBlogPost(blogId, postId, true)
        post?.let {
            if (!post.isExternal) {
                val isRepliesDataChanged = lastRenderedRepliesData?.isMatchingPostCommentsStatus(
                        it.blogId,
                        it.postId,
                        it.numReplies
                ) ?: true

                if (!isRepliesDataChanged) return

                readerCommentServiceStarterWrapper.startServiceForCommentSnippet(
                        contextProvider.getContext(),
                        blogId,
                        postId
                )
            }
        }
    }

    fun onRefreshLikersData(post: ReaderPost, isLikingAction: Boolean = false) {
        if (
                !likesEnhancementsFeatureConfig.isEnabled() ||
                readerUtilsWrapper.isExternalFeed(post.blogId, post.feedId)
        ) {
            return
        }

        val isLikeDataChanged = lastRenderedLikesData?.isMatchingPostLikeStatus(post) ?: true

        if (!isLikeDataChanged) return

        lastRenderedLikesData = RenderedLikesData(
                post.blogId,
                post.postId,
                post.numLikes,
                post.isLikedByCurrentUser
        )

        if (isLikingAction) {
            val state = _updateLikesState.value

            state?.let {
                when (it) {
                    is Failure -> _updateLikesState.value = it.copy(
                            iLike = post.isLikedByCurrentUser,
                            expectedNumLikes = post.numLikes
                    )
                    is LikesData -> _updateLikesState.value = it.copy(
                            iLike = post.isLikedByCurrentUser,
                            expectedNumLikes = post.numLikes
                    )
                    Loading -> {}
                }
            }
        } else {
            getLikesJob?.cancel()
            getLikesJob = launch(bgDispatcher) {
                getLikesHandler.handleGetLikesForPost(
                        LikeGroupFingerPrint(
                                post.blogId,
                                post.postId,
                                post.numLikes
                        ),
                        requestNextPage = false,
                        pageLength = MAX_NUM_LIKES_FACES_WITH_SELF
                )
            }
        }
    }

    fun onShowPost(blogId: Long, postId: Long) {
        launch { getOrFetchReaderPost(blogId = blogId, postId = postId) }
    }

    private suspend fun getOrFetchReaderPost(blogId: Long, postId: Long) {
        getReaderPostFromDb(blogId = blogId, postId = postId)
        if (post == null) {
            _uiState.value = LoadingUiState
            when (readerFetchPostUseCase.fetchPost(blogId = blogId, postId = postId, isFeed = isFeed)) {
                FetchReaderPostState.Success -> {
                    getReaderPostFromDb(blogId, postId)
                    updatePostDetailsUi()
                }

                FetchReaderPostState.AlreadyRunning -> {
                    AppLog.i(T.READER, "reader post detail > fetch post already running")
                    _uiState.value = ErrorUiState(null)
                }

                FetchReaderPostState.Failed.NoNetwork ->
                    _uiState.value = ErrorUiState(UiStringRes(R.string.no_network_message))

                FetchReaderPostState.Failed.RequestFailed ->
                    _uiState.value = ErrorUiState(UiStringRes(R.string.reader_err_get_post_generic))

                FetchReaderPostState.Failed.NotAuthorised -> trackAndUpdateNotAuthorisedErrorState()

                FetchReaderPostState.Failed.PostNotFound ->
                    _uiState.value = ErrorUiState(UiStringRes(R.string.reader_err_get_post_not_found))
            }
        } else {
            updatePostDetailsUi()
        }
    }

    private suspend fun getReaderPostFromDb(blogId: Long, postId: Long) {
        val (readerPost, isFeedPost) = readerGetPostUseCase.get(blogId = blogId, postId = postId, isFeed = this.isFeed)
        this.post = readerPost
        this.isFeed = isFeedPost
    }

    fun onNotAuthorisedRequestFailure() {
        trackAndUpdateNotAuthorisedErrorState()
    }

    fun onMoreButtonClicked() {
        changeMoreMenuVisibility(true)
    }

    fun onMoreMenuDismissed() {
        changeMoreMenuVisibility(false)
    }

    fun onMoreMenuItemClicked(type: ReaderPostCardActionType) {
        val currentUiState = (_uiState.value as? ReaderPostDetailsUiState)
        currentUiState?.let {
            onButtonClicked(currentUiState.postId, currentUiState.blogId, type)
        }
        changeMoreMenuVisibility(false)
    }

    private fun changeMoreMenuVisibility(show: Boolean) {
        val currentUiState = (_uiState.value as? ReaderPostDetailsUiState)
        currentUiState?.let {
            findPost(it.postId, it.blogId)?.let { post ->
                val moreMenuItems = if (show) {
                    readerPostMoreButtonUiStateBuilder.buildMoreMenuItemsBlocking(
                            post, this@ReaderPostDetailViewModel::onButtonClicked
                    )
                } else {
                    null
                }

                _uiState.value = it.copy(moreMenuItems = moreMenuItems)
            }
        }
    }

    fun onFeaturedImageClicked(blogId: Long, featuredImageUrl: String) {
        readerTracker.track(Stat.READER_ARTICLE_FEATURED_IMAGE_TAPPED)
        val site = siteStore.getSiteBySiteId(blogId)
        _navigationEvents.value = Event(
                ReaderNavigationEvents.ShowMediaPreview(site = site, featuredImage = featuredImageUrl)
        )
    }

    fun onCommentSnippetClicked(postId: Long, blogId: Long) {
        if (!commentsSnippetFeatureConfig.isEnabled()) return
        onActionClicked(
                postId,
                blogId,
                ReaderPostCardActionType.COMMENTS,
                ReaderTracker.SOURCE_POST_DETAIL_COMMENT_SNIPPET
        )
    }

    fun onButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        onActionClicked(postId, blogId, type, ReaderTracker.SOURCE_POST_DETAIL)
    }

    private fun onActionClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType, source: String) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(
                        it,
                        type,
                        isBookmarkList = false,
                        source = source
                )
            }
        }
    }

    fun onReblogSiteSelected(siteLocalId: Int) {
        launch {
            val state = reblogUseCase.onReblogSiteSelected(siteLocalId, pendingReblogPost)
            val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
            if (navigationTarget != null) {
                _navigationEvents.value = Event(navigationTarget)
            } else {
                _snackbarEvents.value = Event(SnackbarMessageHolder(UiStringRes(R.string.reader_reblog_error)))
            }
            pendingReblogPost = null
        }
    }

    fun onUpdatePost(post: ReaderPost) {
        _uiState.value = convertPostToUiState(post)
    }

    private fun onTagItemClicked(tagSlug: String) {
        launch(ioDispatcher) {
            val readerTag = readerUtilsWrapper.getTagFromTagName(tagSlug, FOLLOWED)
            _navigationEvents.postValue(Event(ShowPostsByTag(readerTag)))
        }
    }

    private fun onBlogSectionClicked(postId: Long, blogId: Long) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.handleHeaderClicked(
                        blogId,
                        it.feedId,
                        it.isFollowedByCurrentUser
                )
            }
        }
    }

    fun onVisitPostExcerptFooterClicked(postLink: String) {
        _navigationEvents.value = Event(ReaderNavigationEvents.OpenUrl(url = postLink))
    }

    private fun onRelatedPostItemClicked(postId: Long, blogId: Long, isGlobal: Boolean) {
        trackRelatedPostClickAction(postId, blogId, isGlobal)
        _navigationEvents.value = if (isRelatedPost) {
            Event(ReplaceRelatedPostDetailsWithHistory(postId = postId, blogId = blogId, isGlobal = isGlobal))
        } else {
            Event(ShowRelatedPostDetails(postId = postId, blogId = blogId))
        }
    }

    fun onRelatedPostsRequested(sourcePost: ReaderPost) {
        /* Related posts only available for wp.com */
        if (!sourcePost.isWP) return

        launch {
            when (val fetchRelatedPostsState = readerFetchRelatedPostsUseCase.fetchRelatedPosts(sourcePost)) {
                is FetchRelatedPostsState.AlreadyRunning,
                is FetchRelatedPostsState.Failed.NoNetwork,
                is FetchRelatedPostsState.Failed.RequestFailed -> Unit // Do Nothing
                is FetchRelatedPostsState.Success -> updateRelatedPostsUiState(sourcePost, fetchRelatedPostsState)
            }
        }
    }

    private fun trackRelatedPostClickAction(postId: Long, blogId: Long, isGlobal: Boolean) {
        val stat = if (isGlobal) {
            AnalyticsTracker.Stat.READER_GLOBAL_RELATED_POST_CLICKED
        } else {
            AnalyticsTracker.Stat.READER_LOCAL_RELATED_POST_CLICKED
        }
        readerTracker.trackPost(stat, findPost(blogId, postId))
    }

    private fun findPost(postId: Long, blogId: Long): ReaderPost? {
        return readerPostTableWrapper.getBlogPost(
                blogId,
                postId,
                true
        )
    }

    private fun convertPostToUiState(
        post: ReaderPost
    ): ReaderPostDetailsUiState {
        return postDetailUiStateBuilder.mapPostToUiState(
                post = post,
                onButtonClicked = this@ReaderPostDetailViewModel::onButtonClicked,
                onBlogSectionClicked = this@ReaderPostDetailViewModel::onBlogSectionClicked,
                onFollowClicked = { onButtonClicked(post.postId, post.blogId, FOLLOW) },
                onTagItemClicked = this@ReaderPostDetailViewModel::onTagItemClicked
        )
    }

    private fun convertRelatedPostsToUiState(
        sourcePost: ReaderPost,
        relatedPosts: ReaderSimplePostList,
        isGlobal: Boolean
    ) = postDetailUiStateBuilder.mapRelatedPostsToUiState(
            sourcePost = sourcePost,
            relatedPosts = relatedPosts,
            isGlobal = isGlobal,
            onItemClicked = this@ReaderPostDetailViewModel::onRelatedPostItemClicked
    )

    private fun updatePostDetailsUi() {
        post?.let {
            readerTracker.trackPost(AnalyticsTracker.Stat.READER_ARTICLE_RENDERED, it)
            _navigationEvents.postValue(Event(ShowPostInWebView(it)))
            _uiState.value = convertPostToUiState(it)
        }
    }

    private fun updateFollowButtonUiState(
        currentUiState: ReaderPostDetailsUiState,
        isFollowed: Boolean
    ) {
        val updatedFollowButtonUiState = currentUiState
                .headerUiState
                .followButtonUiState
                .copy(isFollowed = isFollowed)

        val updatedHeaderUiState = currentUiState
                .headerUiState
                .copy(followButtonUiState = updatedFollowButtonUiState)

        _uiState.value = currentUiState.copy(headerUiState = updatedHeaderUiState)
    }

    private fun updatePostActions(post: ReaderPost) {
        (_uiState.value as? ReaderPostDetailsUiState)?.let {
            _uiState.value = it.copy(
                    actions = postDetailUiStateBuilder.buildPostActions(
                            post,
                            this@ReaderPostDetailViewModel::onButtonClicked
                    )
            )
        }
    }

    private fun updateRelatedPostsUiState(sourcePost: ReaderPost, state: FetchRelatedPostsState.Success) {
        (_uiState.value as? ReaderPostDetailsUiState)?.let {
            _uiState.value = it.copy(
                    localRelatedPosts = convertRelatedPostsToUiState(
                            sourcePost = sourcePost,
                            relatedPosts = state.localRelatedPosts,
                            isGlobal = false
                    ),
                    globalRelatedPosts = convertRelatedPostsToUiState(
                            sourcePost = sourcePost,
                            relatedPosts = state.globalRelatedPosts,
                            isGlobal = true
                    )
            )
        }
    }

    private fun trackAndUpdateNotAuthorisedErrorState() {
        trackNotAuthorisedState()

        _uiState.value = ErrorUiState(
                message = UiStringRes(getNotAuthorisedErrorMessageRes()),
                signInButtonVisibility = shouldOfferSignIn
        )
    }

    private fun trackNotAuthorisedState() {
        if (shouldOfferSignIn) {
            post?.let { readerTracker.trackPost(AnalyticsTracker.Stat.READER_WPCOM_SIGN_IN_NEEDED, it) }
        }
        post?.let { readerTracker.trackPost(AnalyticsTracker.Stat.READER_USER_UNAUTHORIZED, it) }
    }

    private fun getNotAuthorisedErrorMessageRes() = if (!shouldOfferSignIn) {
        if (interceptedUri == null) {
            R.string.reader_err_get_post_not_authorized
        } else {
            R.string.reader_err_get_post_not_authorized_fallback
        }
    } else {
        if (interceptedUri == null) {
            R.string.reader_err_get_post_not_authorized_signin
        } else {
            R.string.reader_err_get_post_not_authorized_signin_fallback
        }
    }

    private fun buildLikersUiState(updateLikesState: GetLikesState?): TrainOfFacesUiState {
        val (likers, numLikes, iLiked) = getLikersEssentials(updateLikesState)

        val showLoading = updateLikesState is Loading
        var showEmptyState = false
        var emptyStateTitle: UiString? = null

        if (updateLikesState is Failure && !showLoading) {
            updateLikesState.emptyStateData?.let {
                showEmptyState = it.showEmptyState
                emptyStateTitle = it.title
            }
        }

        val showLikeFacesTrainContainer = post?.let {
            it.isWP && ((numLikes > 0 && (likers.isNotEmpty() || showEmptyState)) || showLoading)
        } ?: false

        val engageItemsList = if (showLikeFacesTrainContainer) {
            likers + getLikersFacesText(showEmptyState, numLikes, iLiked)
        } else {
            listOf()
        }

        val goingToShowFaces = showLikeFacesTrainContainer && !showEmptyState

        val contentDescription = getContentDescription(goingToShowFaces, engageItemsList)

        return TrainOfFacesUiState(
                showLikeFacesTrainContainer = showLikeFacesTrainContainer,
                showLoading = showLoading,
                engageItemsList = engageItemsList,
                showEmptyState = showEmptyState,
                emptyStateTitle = emptyStateTitle,
                contentDescription = contentDescription,
                goingToShowFaces = goingToShowFaces
        )
    }

    private fun getContentDescription(
        goingToShowFaces: Boolean,
        items: List<TrainOfAvatarsItem>
    ) = if (goingToShowFaces) {
        when (val lastItem = items.lastOrNull()) {
            is TrailingLabelTextItem -> lastItem.text
            is AvatarItem, null -> UiStringText("")
        }
    } else {
        UiStringText("")
    }

    @Suppress("LongMethod")
    private fun getLikersFacesText(showEmptyState: Boolean, numLikes: Int, iLiked: Boolean): List<TrainOfAvatarsItem> {
        @AttrRes val labelColor = R.attr.wpColorOnSurfaceMedium
        return when {
            showEmptyState -> {
                listOf()
            }
            numLikes == 1 && iLiked -> {
                TrailingLabelTextItem(
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(R.string.like_faces_you_like_text)
                        ),
                        labelColor
                ).toList()
            }
            numLikes == 2 && iLiked -> {
                TrailingLabelTextItem(
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        R.string.like_faces_you_plus_one_like_text
                                )
                        ),
                        labelColor
                ).toList()
            }
            numLikes > 2 && iLiked -> {
                TrailingLabelTextItem(
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        R.string.like_faces_you_plus_others_like_text,
                                        numLikes - 1
                                )
                        ),
                        labelColor
                ).toList()
            }
            numLikes == 1 && !iLiked -> {
                TrailingLabelTextItem(
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        R.string.like_faces_one_blogger_likes_text
                                )
                        ),
                        labelColor
                ).toList()
            }
            numLikes > 1 && !iLiked -> {
                TrailingLabelTextItem(
                        UiStringText(
                                htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                                        R.string.like_faces_others_like_text,
                                        numLikes
                                )
                        ),
                        labelColor
                ).toList()
            }
            else -> {
                listOf()
            }
        }
    }

    private fun TrailingLabelTextItem.toList() = listOf(this)

    private fun getLikersEssentials(updateLikesState: GetLikesState?): Triple<List<TrainOfAvatarsItem>, Int, Boolean> {
        return when (updateLikesState) {
            is LikesData -> {
                val liked = isLikedByCurrentUser(updateLikesState.iLike)
                Triple(
                        engagementUtils.likesToTrainOfFaces(updateLikesState.likes.manageSelfLike(liked)),
                        updateLikesState.expectedNumLikes,
                        liked
                )
            }
            is Failure -> {
                val liked = isLikedByCurrentUser(updateLikesState.iLike)
                Triple(
                        engagementUtils.likesToTrainOfFaces(updateLikesState.cachedLikes.manageSelfLike(liked)),
                        updateLikesState.expectedNumLikes,
                        liked
                )
            }
            Loading, null -> Triple(listOf(), 0, false)
        }
    }

    private fun List<LikeModel>.manageSelfLike(iLiked: Boolean): List<LikeModel> {
        return this.take(MAX_NUM_LIKES_FACES_WITH_SELF).filter {
            it.likerId != accountStore.account.userId
        }.take(MAX_NUM_LIKES_FACES_WITHOUT_SELF).let { likersList ->
            if (iLiked) {
                likersList + LikeModel().apply {
                    likerId = accountStore.account.userId
                    likerAvatarUrl = accountStore.account.avatarUrl
                }
            } else {
                likersList
            }
        }
    }

    private fun isLikedByCurrentUser(iLiked: Boolean?): Boolean {
        return iLiked ?: post?.isLikedByCurrentUser ?: false
    }

    fun onLikeFacesClicked() {
        post?.let { readerPost ->
            _navigationEvents.value = Event(
                    ShowEngagedPeopleList(
                            readerPost.blogId,
                            readerPost.postId,
                            HeaderData(
                                    AuthorNameString(readerPost.authorName),
                                    readerPost.title,
                                    readerPost.postAvatar,
                                    readerPost.authorId,
                                    readerPost.authorBlogId,
                                    readerPost.authorBlogUrl,
                                    lastRenderedLikesData?.numLikes ?: readerPost.numLikes
                            )
                    )
            )
        }
    }

    sealed class UiState(
        val loadingVisible: Boolean = false,
        val errorVisible: Boolean = false
    ) {
        object LoadingUiState : UiState(loadingVisible = true)

        data class ErrorUiState(
            val message: UiString?,
            val signInButtonVisibility: Boolean = false
        ) : UiState(errorVisible = true)

        data class ReaderPostDetailsUiState(
            val postId: Long,
            val blogId: Long,
            val featuredImageUiState: ReaderPostFeaturedImageUiState? = null,
            val headerUiState: ReaderPostDetailsHeaderUiState,
            val excerptFooterUiState: ExcerptFooterUiState?,
            val moreMenuItems: List<ReaderPostCardAction>? = null,
            val actions: ReaderPostActions,
            val localRelatedPosts: RelatedPostsUiState? = null,
            val globalRelatedPosts: RelatedPostsUiState? = null
        ) : UiState() {
            data class ReaderPostFeaturedImageUiState(val blogId: Long, val url: String? = null, val height: Int)

            data class ExcerptFooterUiState(val visitPostExcerptFooterLinkText: UiString? = null, val postLink: String?)

            data class RelatedPostsUiState(
                val cards: List<ReaderRelatedPostUiState>?,
                val isGlobal: Boolean,
                val headerLabel: UiString?,
                val railcarJsonStrings: List<String?>
            ) {
                data class ReaderRelatedPostUiState(
                    val postId: Long,
                    val blogId: Long,
                    val isGlobal: Boolean,
                    val title: UiString?,
                    val excerpt: UiString?,
                    val featuredImageUrl: String?,
                    val featuredImageVisibility: Boolean,
                    val featuredImageCornerRadius: UiDimen,
                    val onItemClicked: (Long, Long, Boolean) -> Unit
                )
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdateCommentsStarted?) {
        if (!commentsSnippetFeatureConfig.isEnabled() || event == null || event.scenario != COMMENT_SNIPPET) return
        if (post?.blogId != event.blogId || post?.postId != event.postId) return

        _commentSnippetState.value = CommentSnippetState.Loading
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onEventMainThread(event: UpdateCommentsEnded?) {
        if (!commentsSnippetFeatureConfig.isEnabled() || event == null || event.scenario != COMMENT_SNIPPET) return
        if (event.result == null || post?.blogId != event.blogId || post?.postId != event.postId) return

        launch(mainDispatcher) {
            // Check the cache
            val comments: ReaderCommentList? = post?.let {
                withContext(bgDispatcher) {
                    readerCommentTableWrapper.getCommentsForPostSnippet(
                            it,
                            READER_COMMENTS_TO_REQUEST_FOR_POST_SNIPPET
                    ) ?: ReaderCommentList()
                }
            }

            _commentSnippetState.value = getUpdatedSnippetState(comments, event.result)
        }
    }

    fun onUserNavigateFromComments() {
        // reload post from DB and update UI state
        val currentUiState: ReaderPostDetailsUiState? = (_uiState.value as? ReaderPostDetailsUiState)
        currentUiState?.let {
            findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                this.post = post
                onUpdatePost(post)
            }
        }

        if (commentsSnippetFeatureConfig.isEnabled()) {
            // reload comments from DB and update comments snippet if they are not being loaded
            if (_commentSnippetState.value !is CommentSnippetState.Loading) {
                launch(mainDispatcher) {
                    val comments: ReaderCommentList? = post?.let {
                        withContext(bgDispatcher) {
                            readerCommentTableWrapper.getCommentsForPostSnippet(
                                    it,
                                    READER_COMMENTS_TO_REQUEST_FOR_POST_SNIPPET
                            ) ?: ReaderCommentList()
                        }
                    }

                    _commentSnippetState.value = getUpdatedSnippetState(comments, CHANGED)
                }
            }
        }
    }

    private fun getUpdatedSnippetState(comments: ReaderCommentList?, result: UpdateResult): CommentSnippetState {
        return if (comments == null) {
            lastRenderedRepliesData = null
            CommentSnippetState.Failure(UiStringRes(R.string.reader_comments_post_fetch_failure))
        } else {
            when (result) {
                HAS_NEW, CHANGED, UNCHANGED -> {
                    lastRenderedRepliesData = RenderedRepliesData(
                            blogId = post?.blogId,
                            postId = post?.postId,
                            numReplies = post?.numReplies
                    )

                    if (comments.isNotEmpty()) {
                        CommentSnippetData(comments = comments)
                    } else {
                        CommentSnippetState.Empty(UiStringRes(
                                if (post?.isCommentsOpen != false) {
                                    R.string.reader_empty_comments
                                } else {
                                    R.string.reader_label_comments_closed
                                }
                        ))
                    }
                }
                FAILED -> {
                    lastRenderedRepliesData = null
                    if (!networkUtilsWrapper.isNetworkAvailable()) {
                        CommentSnippetState.Failure(UiStringRes(R.string.no_network_message))
                    } else {
                        CommentSnippetState.Failure(UiStringRes(R.string.reader_comments_fetch_failure))
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getLikesJob?.cancel()
        getLikesHandler.clear()
        readerPostCardActionsHandler.onCleared()
        eventBusWrapper.unregister(readerFetchRelatedPostsUseCase)
        if (commentsSnippetFeatureConfig.isEnabled()) {
            eventBusWrapper.unregister(this)
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_NUM_LIKES_FACES_WITH_SELF = 6
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MAX_NUM_LIKES_FACES_WITHOUT_SELF = 5
    }
}
