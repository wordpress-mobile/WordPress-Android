package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
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
    private val readerPostTableWrapper: ReaderPostTableWrapper,
) : ScopedViewModel(bgDispatcher) {
    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow

    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

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
            initNavigationEvents()
        }

        // Initially add all tags to the list with the posts loading UI
        _uiStateFlow.update {
            readerTagsFeedUiStateMapper.mapInitialPostsUiState(tags, ::onTagClick, ::onItemEnteredView)
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
                onTagClick = ::onTagClick,
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
                    onTagClick = ::onTagClick,
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
                    onTagClick = ::onTagClick,
                    onRetryClick = ::onRetryClick,
                    onItemEnteredView = ::onItemEnteredView,
                )
            }
        } catch (e: ReaderPostFetchException) {
            readerTagsFeedUiStateMapper.mapErrorTagFeedItem(
                tag = tag,
                errorType = ErrorType.Default,
                onTagClick = ::onTagClick,
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
            // We need it's index to update it and keep it in place.
            val existingIndex = updatedLoadedData.indexOfFirst { it.tagChip.tag == updatedItem.tagChip.tag }

            // Remove the current row(s) of this tag, so we don't have duplicates.
            updatedLoadedData.removeAll { it.tagChip.tag == updatedItem.tagChip.tag }

            // Add the updated item in the correct position.
            updatedLoadedData.add(existingIndex, updatedItem)

            UiState.Loaded(updatedLoadedData)
        }
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

    private fun onOpenTagsListClick() {
        // TODO
    }

    @VisibleForTesting
    fun onTagClick(readerTag: ReaderTag) {
        _actionEvents.value = ActionEvent.OpenTagPostsFeed(readerTag)
    }

    private fun onRetryClick() {
        // TODO
    }

    @VisibleForTesting
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

    private fun onPostLikeClick() {
        // TODO
    }

    private fun onPostMoreMenuClick() {
        // TODO
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
        data object Initial : UiState()
        data class Loaded(val data: List<TagFeedItem>) : UiState()

        data object Loading : UiState()

        data class Empty(val onOpenTagsListClick: () -> Unit) : UiState()
    }

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
        val onTagClick: (ReaderTag) -> Unit,
    )

    sealed class PostList {
        data object Initial : PostList()

        data class Loaded(val items: List<TagsFeedPostItem>) : PostList()

        data object Loading : PostList()

        data class Error(
            val type: ErrorType,
            val onRetryClick: () -> Unit
        ) : PostList()
    }

    sealed interface ErrorType {
        data object Default : ErrorType

        data object NoContent : ErrorType
    }
}
