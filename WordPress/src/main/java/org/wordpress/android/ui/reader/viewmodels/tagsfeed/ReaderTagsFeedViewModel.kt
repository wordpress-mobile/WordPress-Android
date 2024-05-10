package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderTypes
import org.wordpress.android.ui.reader.discover.FEATURED_IMAGE_HEIGHT_WIDTH_RATION
import org.wordpress.android.ui.reader.discover.PHOTON_WIDTH_QUALITY_RATION
import org.wordpress.android.ui.reader.discover.ReaderCardUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.repository.usecases.PostLikeUseCase
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderTagsFeedViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val readerPostRepository: ReaderPostRepository,
    private val readerTagsFeedUiStateMapper: ReaderTagsFeedUiStateMapper,
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val postLikeUseCase: PostLikeUseCase,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder,
    private val readerPostUiStateBuilder: ReaderPostUiStateBuilder,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
) : ScopedViewModel(bgDispatcher) {
    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow

    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    // Unlike the snackbarEvents observable which only expects messages from ReaderPostCardActionsHandler,
    // this observable is controlled by this ViewModel.
    private val _errorMessageEvents = MediatorLiveData<Event<Int>>()
    val errorMessageEvents: LiveData<Event<Int>> = _errorMessageEvents

    // This observable just expects messages from ReaderPostCardActionsHandler. Nothing is directly triggered
    // from this ViewModel.
    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _openMoreMenuEvents = SingleLiveEvent<MoreMenuUiState>()
    val openMoreMenuEvents: LiveData<MoreMenuUiState> = _openMoreMenuEvents

    private var hasInitialized = false

    /**
     * Fetch multiple tag posts in parallel. Each tag load causes a new state to be emitted, so multiple emissions of
     * [uiStateFlow] are expected when calling this method for each tag, since each can go through the following
     * [UiState]s: [UiState.Initial], [UiState.Loaded], [UiState.Loading], [UiState.Empty].
     */
    fun start(tags: List<ReaderTag>) {
        // don't start again if the tags match
        if (_uiStateFlow.value is UiState.Loaded &&
            tags == (_uiStateFlow.value as UiState.Loaded).data.map { it.tagChip.tag }
        ) {
            return
        }

        if (tags.isEmpty()) {
            _uiStateFlow.value = UiState.Empty(::onOpenTagsListClick)
            return
        }

        if (!hasInitialized) {
            hasInitialized = true
            readerPostCardActionsHandler.initScope(viewModelScope)
            initNavigationEvents()
            initSnackbarEvents()
        }

        // Initially add all tags to the list with the posts loading UI
        _uiStateFlow.update {
            readerTagsFeedUiStateMapper.mapLoadingPostsUiState(tags, ::onTagClick)
        }
        // Fetch all posts and update the posts loading UI to either loaded or error when the request finishes
        launch {
            tags.forEach {
                fetchTag(it)
            }
        }
    }

    private fun initNavigationEvents() {
        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            // TODO reblog supported in this screen? See ReaderPostDetailViewModel and ReaderDiscoverViewModel
//            val target = event.peekContent()
//            if (target is ReaderNavigationEvents.ShowSitePickerForResult) {
//                pendingReblogPost = target.post
//            }
            _navigationEvents.value = event
        }
    }

    private fun initSnackbarEvents() {
        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    /**
     * Fetch posts for a single tag. This method will emit a new state to [uiStateFlow] for different [UiState]s:
     * [UiState.Initial], [UiState.Loaded], [UiState.Loading], [UiState.Empty], but only for the tag being fetched.
     *
     * Can be used for retrying a failed fetch, for instance.
     */
    @Suppress("SwallowedException")
    private suspend fun fetchTag(tag: ReaderTag) {
        val updatedLoadedData = getUpdatedLoadedData()
        // At this point, all tag feed items already exist in the UI with the loading status.
        // We need it's index to update it to either Loaded or Error when the request is finished.
        val existingIndex = updatedLoadedData.indexOfFirst { it.tagChip.tag == tag }
        // Remove the current row of this tag (which is loading). This will be used to later add an updated item with
        // either Loaded or Error status, depending on the result of the request.
        updatedLoadedData.removeAll { it.tagChip.tag == tag }
        try {
            // Fetch posts for tag
            val posts = readerPostRepository.fetchNewerPostsForTag(tag)
            if (posts.isNotEmpty()) {
                updatedLoadedData.add(
                    existingIndex,
                    readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(
                        tag = tag,
                        posts = posts,
                        onTagClick = ::onTagClick,
                        onSiteClick = ::onSiteClick,
                        onPostCardClick = ::onPostCardClick,
                        onPostLikeClick = ::onPostLikeClick,
                        onPostMoreMenuClick = ::onPostMoreMenuClick,
                    )
                )
            } else {
                updatedLoadedData.add(
                    existingIndex,
                    readerTagsFeedUiStateMapper.mapErrorTagFeedItem(
                        tag = tag,
                        errorType = ErrorType.NoContent,
                        onTagClick = ::onTagClick,
                        onRetryClick = ::onRetryClick,
                    )
                )
            }
        } catch (e: ReaderPostFetchException) {
            updatedLoadedData.add(
                existingIndex,
                readerTagsFeedUiStateMapper.mapErrorTagFeedItem(
                    tag = tag,
                    errorType = ErrorType.Default,
                    onTagClick = ::onTagClick,
                    onRetryClick = ::onRetryClick,
                )
            )
        }
        _uiStateFlow.update { UiState.Loaded(updatedLoadedData) }
    }

    private fun getUpdatedLoadedData(): MutableList<TagFeedItem> {
        val updatedLoadedData = mutableListOf<TagFeedItem>()
        val currentUiState = _uiStateFlow.value
        if (currentUiState is UiState.Loaded) {
            val currentLoadedData = currentUiState.data
            updatedLoadedData.addAll(currentLoadedData)
        }
        return updatedLoadedData
    }

    private fun onOpenTagsListClick() {
        // TODO
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onTagClick(readerTag: ReaderTag) {
        _actionEvents.value = ActionEvent.OpenTagPostsFeed(readerTag)
    }

    private fun onRetryClick() {
        // TODO
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onSiteClick(postItem: TagsFeedPostItem) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let {
                _navigationEvents.postValue(
                    Event(ReaderNavigationEvents.ShowBlogPreview(it.blogId, it.feedId, it.isFollowedByCurrentUser))
                )
            }
        }
    }

    private fun onPostCardClick(postItem: TagsFeedPostItem) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let {
                readerPostCardActionsHandler.handleOnItemClicked(
                    it,
                    ReaderTracker.SOURCE_TAGS_FEED
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onPostLikeClick(postItem: TagsFeedPostItem) {
        // Immediately update the UI and disable the like button. If the request fails, show error and revert UI state.
        // If the request fails or succeeds, the like button is enabled again.
        val isPostLikedUpdated = !postItem.isPostLiked
        updatePostItemUI(
            postItemToUpdate = postItem,
            isPostLikedUpdated = isPostLikedUpdated,
            isLikeButtonEnabled = false,
        )

        // After updating the like button UI to the intended state and disabling the like button, send a request to the
        // like endpoint by using the PostLikeUseCase
        likePostRemote(postItem, isPostLikedUpdated)
    }

    private fun updatePostItemUI(
        postItemToUpdate: TagsFeedPostItem,
        isPostLikedUpdated: Boolean,
        isLikeButtonEnabled: Boolean,
    ) {
        val uiState = _uiStateFlow.value as? UiState.Loaded ?: return
        // Finds the TagFeedItem associated with the post that should be updated. Return if the item is
        // not found.
        val tagFeedItemToUpdate = findTagFeedItemToUpdate(uiState, postItemToUpdate) ?: return

        // Finds the index associated with the TagFeedItem to be updated found above. Return if the index is not found.
        val tagFeedItemToUpdateIndex = uiState.data.indexOf(tagFeedItemToUpdate)
        if (tagFeedItemToUpdateIndex != -1 && tagFeedItemToUpdate.postList is PostList.Loaded) {
            // Creates a new post list items collection with the post item updated values
            val updatedTagFeedItemPostListItems = getPostListWithUpdatedPostItem(
                postList = tagFeedItemToUpdate.postList,
                postItemToUpdate = postItemToUpdate,
                isPostLikedUpdated = isPostLikedUpdated,
                isLikeButtonEnabled = isLikeButtonEnabled,
            )
            // Creates a copy of the TagFeedItem with the updated post list items collection
            val updatedTagFeedItem = tagFeedItemToUpdate.copy(
                postList = tagFeedItemToUpdate.postList.copy(
                    items = updatedTagFeedItemPostListItems
                )
            )
            // Creates a new TagFeedItem collection with the updated TagFeedItem
            val updatedUiStateData = mutableListOf<TagFeedItem>().apply {
                addAll(uiState.data)
                this[tagFeedItemToUpdateIndex] = updatedTagFeedItem
            }
            // Updates the UI state value with the updated TagFeedItem collection
            _uiStateFlow.value = uiState.copy(data = updatedUiStateData)
        }
    }

    private fun getPostListWithUpdatedPostItem(
        postList: PostList.Loaded,
        postItemToUpdate: TagsFeedPostItem,
        isPostLikedUpdated: Boolean,
        isLikeButtonEnabled: Boolean
    ) =
        postList.items.toMutableList().apply {
            val postItemToUpdateIndex =
                indexOfFirst {
                    it.postId == postItemToUpdate.postId && it.blogId == postItemToUpdate.blogId
                }
            if (postItemToUpdateIndex != -1) {
                this[postItemToUpdateIndex] = postItemToUpdate.copy(
                    isPostLiked = isPostLikedUpdated,
                    isLikeButtonEnabled = isLikeButtonEnabled,
                )
            }
        }

    private fun findTagFeedItemToUpdate(uiState: UiState.Loaded, postItemToUpdate: TagsFeedPostItem) =
        uiState.data.firstOrNull { tagFeedItem ->
            tagFeedItem.postList is PostList.Loaded && tagFeedItem.postList.items.firstOrNull {
                it.postId == postItemToUpdate.postId && it.blogId == postItemToUpdate.blogId
            } != null
        }

    private fun likePostRemote(postItem: TagsFeedPostItem, isPostLikedUpdated: Boolean) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let {
                postLikeUseCase.perform(it, !it.isLikedByCurrentUser, ReaderTracker.SOURCE_TAGS_FEED).collect {
                    when (it) {
                        is PostLikeUseCase.PostLikeState.Success -> {
                            // Re-enable like button without changing the current post item UI.
                            updatePostItemUI(
                                postItemToUpdate = postItem,
                                isPostLikedUpdated = isPostLikedUpdated,
                                isLikeButtonEnabled = true,
                            )
                        }

                        is PostLikeUseCase.PostLikeState.Failed.NoNetwork -> {
                            // Revert post item like button UI to the previous state and re-enable like button.
                            updatePostItemUI(
                                postItemToUpdate = postItem,
                                isPostLikedUpdated = !isPostLikedUpdated,
                                isLikeButtonEnabled = true,
                            )
                            _errorMessageEvents.postValue(Event(R.string.no_network_message))
                        }

                        is PostLikeUseCase.PostLikeState.Failed.RequestFailed -> {
                            // Revert post item like button UI to the previous state and re-enable like button.
                            updatePostItemUI(
                                postItemToUpdate = postItem,
                                isPostLikedUpdated = !isPostLikedUpdated,
                                isLikeButtonEnabled = true,
                            )
                            _errorMessageEvents.postValue(Event(R.string.reader_error_request_failed_title))
                        }

                        else -> {
                            // no-op
                        }
                    }
                }
            }
        }
    }

    private fun onPostMoreMenuClick(postItem: TagsFeedPostItem) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let { post ->
                val items = readerPostMoreButtonUiStateBuilder.buildMoreMenuItems(
                    post = post,
                    includeBookmark = true,
                    onButtonClicked = ::onMoreMenuButtonClicked,
                )
                val photonWidth = (displayUtilsWrapper.getDisplayPixelWidth() * PHOTON_WIDTH_QUALITY_RATION).toInt()
                val photonHeight = (photonWidth * FEATURED_IMAGE_HEIGHT_WIDTH_RATION).toInt()
                _openMoreMenuEvents.postValue(
                    MoreMenuUiState(
                        readerCardUiState = readerPostUiStateBuilder.mapPostToNewUiState(
                            source = ReaderTracker.SOURCE_TAGS_FEED,
                            post = post,
                            photonWidth = photonWidth,
                            photonHeight = photonHeight,
                            postListType = ReaderTypes.ReaderPostListType.TAGS_FEED,
                            onButtonClicked = { _, _, _ -> },
                            onItemClicked = { _, _ -> },
                            onItemRendered = {},
                            onMoreButtonClicked = {},
                            onMoreDismissed = {},
                            onVideoOverlayClicked = { _, _ -> },
                            onPostHeaderViewClicked = { _, _ -> },
                        ),
                        readerPostCardActions = items,
                    )
                )
            }
        }
    }

    private fun onMoreMenuButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(
                    it,
                    type,
                    isBookmarkList = false,
                    source = ReaderTracker.SOURCE_DISCOVER
                )
            }
        }
    }

    private fun findPost(postId: Long, blogId: Long): ReaderPost? {
        return readerPostTableWrapper.getBlogPost(
            blogId = blogId,
            postId = postId,
            excludeTextColumn = true,
        )
    }

    sealed class ActionEvent {
        data class OpenTagPostsFeed(val readerTag: ReaderTag) : ActionEvent()
    }

    sealed class UiState {
        object Initial : UiState()
        data class Loaded(val data: List<TagFeedItem>) : UiState()

        object Loading : UiState()

        data class Empty(val onOpenTagsListClick: () -> Unit) : UiState()
    }

    data class TagFeedItem(
        val tagChip: TagChip,
        val postList: PostList,
    )

    data class TagChip(
        val tag: ReaderTag,
        val onTagClick: (ReaderTag) -> Unit,
    )

    sealed class PostList {
        data class Loaded(val items: List<TagsFeedPostItem>) : PostList()

        object Loading : PostList()

        data class Error(
            val type: ErrorType,
            val onRetryClick: () -> Unit
        ) : PostList()
    }

    sealed interface ErrorType {
        data object Default : ErrorType

        data object NoContent : ErrorType
    }

    data class MoreMenuUiState(
        val readerCardUiState: ReaderCardUiState.ReaderPostNewUiState,
        val readerPostCardActions: List<ReaderPostCardAction>,
    )
}
