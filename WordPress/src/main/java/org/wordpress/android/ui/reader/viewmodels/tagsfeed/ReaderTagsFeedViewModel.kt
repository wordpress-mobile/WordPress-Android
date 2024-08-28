package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
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
import org.wordpress.android.ui.reader.utils.ReaderAnnouncementHelper
import org.wordpress.android.ui.reader.views.compose.ReaderAnnouncementCardItemData
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
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
    private val readerTracker: ReaderTracker,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val readerAnnouncementHelper: ReaderAnnouncementHelper,
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

    fun onViewCreated() {
        if (!hasInitialized) {
            hasInitialized = true
            readerPostCardActionsHandler.initScope(viewModelScope)
            initNavigationEvents()
            initSnackbarEvents()
            initUiState()
        }
    }

    fun onTagsChanged(tags: List<ReaderTag>) {
        return _uiStateFlow.update { currentState ->
            when {
                tags.isEmpty() -> {
                    UiState.Empty(::onOpenTagsListClick)
                }

                currentState is UiState.Loaded -> {
                    val currentTags = currentState.data.map { it.tagChip.tag }
                    if (currentState.isRefreshing) {
                        readerTagsFeedUiStateMapper.mapInitialPostsUiState(
                            tags,
                            getAnnouncementItem(),
                            false,
                            ::onTagChipClick,
                            ::onMoreFromTagClick,
                            ::onItemEnteredView,
                            ::onRefresh
                        )
                    } else if (currentTags != tags) {
                        updateLoadedStateWithTags(currentState, tags)
                    } else {
                        currentState
                    }
                }

                else -> {
                    // Add tags to the list with the posts initial/loading UI
                    readerTagsFeedUiStateMapper.mapInitialPostsUiState(
                        tags,
                        getAnnouncementItem(),
                        false,
                        ::onTagChipClick,
                        ::onMoreFromTagClick,
                        ::onItemEnteredView,
                        ::onRefresh
                    )
                }
            }
        }
    }

    private fun initNavigationEvents() {
        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            _navigationEvents.value = event
        }
    }

    private fun initSnackbarEvents() {
        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    private fun initUiState() {
        _uiStateFlow.value = if (networkUtilsWrapper.isNetworkAvailable()) {
            UiState.Loading
        } else {
            UiState.NoConnection(::onNoConnectionRetryClick)
        }
    }

    private fun getAnnouncementItem(): ReaderAnnouncementItem? =
        if (readerAnnouncementHelper.hasReaderAnnouncement()) {
            ReaderAnnouncementItem(
                items = readerAnnouncementHelper.getReaderAnnouncementItems(),
                onDoneClicked = ::dismissAnnouncementItem,
            )
        } else {
            null
        }

    private fun dismissAnnouncementItem() {
        readerAnnouncementHelper.dismissReaderAnnouncement()
        _uiStateFlow.update {
            (it as? UiState.Loaded)?.copy(announcementItem = null) ?: it
        }
    }

    private fun updateLoadedStateWithTags(state: UiState.Loaded, tags: List<ReaderTag>): UiState.Loaded {
        val currentTagsMap = state.data.associateBy { it.tagChip.tag.tagSlug }
        val updatedData = tags.map { tag ->
            currentTagsMap[tag.tagSlug] ?: readerTagsFeedUiStateMapper.mapInitialTagFeedItem(
                tag = tag,
                onTagChipClick = ::onTagChipClick,
                onMoreFromTagClick = ::onMoreFromTagClick,
                onItemEnteredView = ::onItemEnteredView,
            )
        }
        return state.copy(data = updatedData)
    }

    private fun onNoConnectionRetryClick() {
        _uiStateFlow.value = UiState.Loading
        if (networkUtilsWrapper.isNetworkAvailable()) {
            _actionEvents.value = ActionEvent.RefreshTags
        } else {
            // delay a bit before returning to NoConnection for a better feedback to the user
            launch {
                delay(NO_CONNECTION_DELAY)
                _uiStateFlow.value = UiState.NoConnection(::onNoConnectionRetryClick)
            }
        }
    }

    /**
     * Fetch posts for a single tag. This method will emit a new state to [uiStateFlow] for different [UiState]s:
     * [UiState.Initial], [UiState.Loaded], [UiState.Loading], [UiState.Empty], but only for the tag being fetched.
     */
    @Suppress("SwallowedException")
    private suspend fun fetchTag(tag: ReaderTag) {
        // Set the tag to loading state
        updateTagFeedItem(
            readerTagsFeedUiStateMapper.mapLoadingTagFeedItem(
                tag = tag,
                onTagChipClick = ::onTagChipClick,
                onMoreFromTagClick = ::onMoreFromTagClick,
                onItemEnteredView = ::onItemEnteredView,
            )
        )

        val updatedItem: TagFeedItem = try {
            // Fetch posts for tag
            val posts = readerPostRepository.fetchNewerPostsForTag(tag)
            if (posts.isNotEmpty()) {
                readerTagsFeedUiStateMapper.mapLoadedTagFeedItem(
                    tag = tag,
                    posts = posts,
                    onTagChipClick = ::onTagChipClick,
                    onMoreFromTagClick = ::onMoreFromTagClick,
                    onSiteClick = ::onSiteClick,
                    onPostCardClick = ::onPostCardClick,
                    onPostLikeClick = ::onPostLikeClick,
                    onPostMoreMenuClick = ::onPostMoreMenuClick,
                    onItemEnteredView = ::onItemEnteredView,
                )
            } else {
                readerTagsFeedUiStateMapper.mapErrorTagFeedItem(
                    tag = tag,
                    errorType = ErrorType.NoContent,
                    onTagChipClick = ::onTagChipClick,
                    onMoreFromTagClick = ::onMoreFromTagClick,
                    onRetryClick = ::onRetryClick,
                    onItemEnteredView = ::onItemEnteredView,
                )
            }
        } catch (e: ReaderPostFetchException) {
            readerTagsFeedUiStateMapper.mapErrorTagFeedItem(
                tag = tag,
                errorType = ErrorType.Default,
                onTagChipClick = ::onTagChipClick,
                onMoreFromTagClick = ::onMoreFromTagClick,
                onRetryClick = ::onRetryClick,
                onItemEnteredView = ::onItemEnteredView,
            )
        }

        updateTagFeedItem(updatedItem)
    }

    private fun getLoadedData(uiState: UiState): MutableList<TagFeedItem> {
        val updatedLoadedData = mutableListOf<TagFeedItem>()
        if (uiState is UiState.Loaded) {
            updatedLoadedData.addAll(uiState.data)
        }
        return updatedLoadedData
    }

    // Update the UI state for a single feed item, making sure to do it atomically so we don't lose any updates.
    private fun updateTagFeedItem(updatedItem: TagFeedItem) {
        _uiStateFlow.update { uiState ->
            val updatedLoadedData = getLoadedData(uiState)

            // At this point, all tag feed items already exist in the UI.
            // We need it's index to update it and keep it in the same place.
            updatedLoadedData.indexOfFirst { it.tagChip.tag == updatedItem.tagChip.tag }
                .takeIf { it >= 0 }
                ?.let { existingIndex ->
                    // Update item
                    updatedLoadedData[existingIndex] = updatedItem
                }

            (uiState as? UiState.Loaded)?.copy(data = updatedLoadedData) ?: UiState.Loaded(
                updatedLoadedData
            )
        }
    }

    @VisibleForTesting
    fun onRefresh() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _errorMessageEvents.postValue(Event(R.string.no_network_message))
            return
        }

        _uiStateFlow.update {
            (it as? UiState.Loaded)?.copy(isRefreshing = true) ?: it
        }
        _actionEvents.value = ActionEvent.RefreshTags
    }

    fun onBackFromTagDetails() {
        if (!networkUtilsWrapper.isNetworkAvailable()) return

        _actionEvents.value = ActionEvent.RefreshTags
    }

    @VisibleForTesting
    fun onItemEnteredView(item: TagFeedItem) {
        if (item.postList != PostList.Initial) {
            // do nothing as it's still loading or already loaded
            return
        }

        launch {
            fetchTag(item.tagChip.tag)
        }
    }

    @VisibleForTesting
    fun onOpenTagsListClick() {
        _actionEvents.value = ActionEvent.ShowTagsList
    }

    @VisibleForTesting
    fun onTagChipClick(readerTag: ReaderTag) {
        readerTracker.track(AnalyticsTracker.Stat.READER_TAGS_FEED_HEADER_TAPPED)
        _actionEvents.value = ActionEvent.FilterTagPostsFeed(readerTag)
    }

    @VisibleForTesting
    fun onMoreFromTagClick(readerTag: ReaderTag) {
        readerTracker.track(AnalyticsTracker.Stat.READER_TAGS_FEED_MORE_FROM_TAG_TAPPED)
        _actionEvents.value = ActionEvent.OpenTagPostList(readerTag)
    }

    @VisibleForTesting
    fun onRetryClick(readerTag: ReaderTag) {
        launch {
            fetchTag(readerTag)
        }
    }

    @VisibleForTesting
    fun onSiteClick(postItem: TagsFeedPostItem) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let {
                _navigationEvents.postValue(
                    Event(
                        ReaderNavigationEvents.ShowBlogPreview(
                            it.blogId,
                            it.feedId,
                            it.isFollowedByCurrentUser
                        )
                    )
                )
            }
        }
    }

    @VisibleForTesting
    fun onPostCardClick(postItem: TagsFeedPostItem) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let {
                readerTracker.trackBlog(
                    AnalyticsTracker.Stat.READER_POST_CARD_TAPPED,
                    it.blogId,
                    it.feedId,
                    it.isFollowedByCurrentUser,
                    ReaderTracker.SOURCE_TAGS_FEED,
                )
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

    private fun findTagFeedItemToUpdate(
        uiState: UiState.Loaded,
        postItemToUpdate: TagsFeedPostItem
    ) =
        uiState.data.firstOrNull { tagFeedItem ->
            tagFeedItem.postList is PostList.Loaded && tagFeedItem.postList.items.firstOrNull {
                it.postId == postItemToUpdate.postId && it.blogId == postItemToUpdate.blogId
            } != null
        }

    private fun likePostRemote(postItem: TagsFeedPostItem, isPostLikedUpdated: Boolean) {
        launch {
            findPost(postItem.postId, postItem.blogId)?.let { post ->
                postLikeUseCase.perform(
                    post,
                    !post.isLikedByCurrentUser,
                    ReaderTracker.SOURCE_TAGS_FEED
                ).collect {
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
                val photonWidth =
                    (displayUtilsWrapper.getDisplayPixelWidth() * PHOTON_WIDTH_QUALITY_RATION).toInt()
                val photonHeight = (photonWidth * FEATURED_IMAGE_HEIGHT_WIDTH_RATION).toInt()
                _openMoreMenuEvents.postValue(
                    MoreMenuUiState(
                        readerCardUiState = readerPostUiStateBuilder.mapPostToUiState(
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

    private fun onMoreMenuButtonClicked(
        postId: Long,
        blogId: Long,
        type: ReaderPostCardActionType
    ) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(
                    it,
                    type,
                    isBookmarkList = false,
                    source = ReaderTracker.SOURCE_TAGS_FEED,
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
        data class FilterTagPostsFeed(val readerTag: ReaderTag) : ActionEvent()

        data class OpenTagPostList(val readerTag: ReaderTag) : ActionEvent()

        data object RefreshTags : ActionEvent()

        data object ShowTagsList : ActionEvent()
    }

    sealed class UiState {
        data object Initial : UiState()

        data class Loaded(
            val data: List<TagFeedItem>,
            val announcementItem: ReaderAnnouncementItem? = null,
            val isRefreshing: Boolean = false,
            val onRefresh: () -> Unit = {},
        ) : UiState()

        data object Loading : UiState()

        data class Empty(val onOpenTagsListClick: () -> Unit) : UiState()

        data class NoConnection(val onRetryClick: () -> Unit) : UiState()
    }

    data class ReaderAnnouncementItem(
        val items: List<ReaderAnnouncementCardItemData>,
        val onDoneClicked: () -> Unit,
    )

    data class TagFeedItem(
        val tagChip: TagChip,
        val postList: PostList,
        private val onItemEnteredView: (TagFeedItem) -> Unit = {},
    ) {
        fun onEnteredView() {
            onItemEnteredView(this)
        }
    }

    data class TagChip(
        val tag: ReaderTag,
        val onTagChipClick: (ReaderTag) -> Unit,
        val onMoreFromTagClick: (ReaderTag) -> Unit,
    )

    sealed class PostList {
        data object Initial : PostList()

        data class Loaded(val items: List<TagsFeedPostItem>) : PostList()

        data object Loading : PostList()

        data class Error(
            val type: ErrorType,
            val onRetryClick: (ReaderTag) -> Unit
        ) : PostList()
    }

    sealed interface ErrorType {
        data object Default : ErrorType

        data object NoContent : ErrorType
    }

    data class MoreMenuUiState(
        val readerCardUiState: ReaderCardUiState.ReaderPostUiState,
        val readerPostCardActions: List<ReaderPostCardAction>,
    )

    companion object {
        private const val NO_CONNECTION_DELAY = 500L
    }
}
