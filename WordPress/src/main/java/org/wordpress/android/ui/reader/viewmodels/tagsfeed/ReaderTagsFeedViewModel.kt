package org.wordpress.android.ui.reader.viewmodels.tagsfeed

import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
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

    /**
     * Fetch multiple tag posts in parallel. Each tag load causes a new state to be emitted, so multiple emissions of
     * [uiStateFlow] are expected when calling this method for each tag, since each can go through the following
     * [UiState]s: [UiState.Initial], [UiState.Loaded], [UiState.Loading], [UiState.Empty].
     */
    fun start(tags: List<ReaderTag>) {
        if (tags.isEmpty()) {
            _uiStateFlow.value = UiState.Empty(::onOpenTagsListClick)
            return
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

    private fun onTagClick(readerTag: ReaderTag) {
        _actionEvents.value = ActionEvent.OpenTagPostsFeed(readerTag)
    }

    private fun onRetryClick() {
        // TODO
    }

    private fun onSiteClick() {
        // TODO
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
}
