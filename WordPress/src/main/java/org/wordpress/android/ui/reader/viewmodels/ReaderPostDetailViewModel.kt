package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.model.LikeModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameString
import org.wordpress.android.ui.engagement.EngageItem
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedPeopleListViewModel.EngagedPeopleListUiState
import org.wordpress.android.ui.engagement.GetLikesHandler
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.InitialLoading
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.HeaderData
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderPostDetailUiStateBuilder
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
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchPostUseCase.FetchReaderPostState
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase
import org.wordpress.android.ui.reader.usecases.ReaderFetchRelatedPostsUseCase.FetchRelatedPostsState
import org.wordpress.android.ui.reader.usecases.ReaderGetPostUseCase
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiDimen
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.WpUrlUtilsWrapper
import org.wordpress.android.util.config.LikesEnhancementsFeatureConfig
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

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
    private val likesEnhancementsFeatureConfig: LikesEnhancementsFeatureConfig
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
    val likesUiState: LiveData<EngagedPeopleListUiState> = _updateLikesState.map {
        state -> buildLikersUiState(state)
    }

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

    private val shouldOfferSignIn: Boolean
        get() = wpUrlUtilsWrapper.isWordPressCom(interceptedUri) && !accountStore.hasAccessToken()

    init {
        eventBusWrapper.register(readerFetchRelatedPostsUseCase)
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
                    updatePostActions(post)
                }
            }
        }

        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _snackbarEvents.addSource(getLikesHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            val target = event.peekContent()
            if (target is ShowSitePickerForResult) {
                pendingReblogPost = target.post
            }
            _navigationEvents.value = event
        }

        _updateLikesState.addSource(getLikesHandler.likesStatusUpdate) { state ->
            _updateLikesState.value = state
        }
    }

    private fun onRefreshLikersData(siteId: Long, postId: Long) {
        getLikesJob?.cancel()
        getLikesJob = launch(bgDispatcher) {
            getLikesHandler.handleGetLikesForPost(siteId, postId)
        }
    }

    fun onShowPost(blogId: Long, postId: Long) {
        launch { getOrFetchReaderPost(blogId = blogId, postId = postId) }
    }

    private suspend fun getOrFetchReaderPost(blogId: Long, postId: Long) {
        _uiState.value = LoadingUiState

        getReaderPostFromDb(blogId = blogId, postId = postId)
        if (post == null) {
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
        val site = siteStore.getSiteBySiteId(blogId)
        _navigationEvents.value = Event(
                ReaderNavigationEvents.ShowMediaPreview(site = site, featuredImage = featuredImageUrl)
        )
    }

    fun onButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(
                        it,
                        type,
                        isBookmarkList = false,
                        source = ReaderTracker.SOURCE_POST_DETAIL
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
        if (likesEnhancementsFeatureConfig.isEnabled() && post.numLikes > 0) {
            onRefreshLikersData(post.blogId, post.postId)
        }

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
        _uiState.value = (_uiState.value as? ReaderPostDetailsUiState)?.copy(
                actions = postDetailUiStateBuilder.buildPostActions(
                        post,
                        this@ReaderPostDetailViewModel::onButtonClicked
                )
        )
    }

    private fun updateRelatedPostsUiState(sourcePost: ReaderPost, state: FetchRelatedPostsState.Success) {
        _uiState.value = (_uiState.value as? ReaderPostDetailsUiState)?.copy(
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

    private fun buildLikersUiState(updateLikesState: GetLikesState?): EngagedPeopleListUiState {
        val likers = when (updateLikesState) {
            is LikesData -> {
                likesToEngagedPeople(updateLikesState.likes)
            }
            is Failure -> {
                likesToEngagedPeople(updateLikesState.cachedLikes)
            }
            InitialLoading, null -> listOf()
        }

        var showEmptyState = false
        var emptyStateTitle: UiString? = null

        if (updateLikesState is Failure) {
            updateLikesState.emptyStateData?.let {
                showEmptyState = it.showEmptyState
                emptyStateTitle = it.title
            }
        }

        return EngagedPeopleListUiState(
                showLikeFacesTrain = post?.let { it.numLikes > 0 } ?: false,
                showLoading = updateLikesState is InitialLoading,
                engageItemsList = likers,
                showEmptyState = showEmptyState,
                numLikes = post?.numLikes ?: 0,
                emptyStateTitle = emptyStateTitle
        )
    }

    private fun likesToEngagedPeople(likes: List<LikeModel>): List<EngageItem> {
        return likes.take(MAX_NUM_LIKES_FACES).map { likeData ->
            Liker(
                    name = likeData.likerName!!,
                    login = likeData.likerLogin!!,
                    userSiteId = likeData.likerSiteId,
                    userSiteUrl = likeData.likerSiteUrl!!,
                    userAvatarUrl = likeData.likerAvatarUrl!!,
                    remoteId = likeData.remoteLikeId
            )
        }
    }

    fun onLikeFacesClicked() {
        post?.let { readerPost ->
            _navigationEvents.value = Event(ShowEngagedPeopleList(
                    readerPost.blogId,
                    readerPost.postId,
                    HeaderData(
                            AuthorNameString(readerPost.authorName),
                            readerPost.title,
                            readerPost.postAvatar,
                            readerPost.authorId,
                            readerPost.authorBlogId,
                            readerPost.authorBlogUrl,
                            readerPost.numLikes
                    )
            ))
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

    override fun onCleared() {
        super.onCleared()
        getLikesJob?.cancel()
        getLikesHandler.clear()
        readerPostCardActionsHandler.onCleared()
        eventBusWrapper.unregister(readerFetchRelatedPostsUseCase)
    }

    companion object {
        private const val MAX_NUM_LIKES_FACES = 5
    }
}
