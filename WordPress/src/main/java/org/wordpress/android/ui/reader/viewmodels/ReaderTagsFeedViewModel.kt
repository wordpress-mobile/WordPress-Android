package org.wordpress.android.ui.reader.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.exceptions.ReaderPostFetchException
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderTagsFeedViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val readerPostRepository: ReaderPostRepository,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
) : ScopedViewModel(bgDispatcher) {
    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow

    /**
     * Fetch multiple tag posts in parallel. Each tag load causes a new state to be emitted, so multiple emissions of
     * [uiStateFlow] are expected when calling this method for each tag, since each can go through the following
     * [UiState]s: [UiState.Initial], [UiState.Loaded], [UiState.Loading], [UiState.Empty].
     */
    fun fetchAll(tags: List<ReaderTag>) {
        if (tags.isEmpty()) {
            _uiStateFlow.value = UiState.Empty(::onOpenTagsListClick)
            return
        }
        // Initially add all tags to the list with the posts loading UI
        loadingTagsUiState(tags)
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
                updatedLoadedData.add(existingIndex, loadedTagFeedItem(tag, posts))
            } else {
                updatedLoadedData.add(
                    existingIndex,
                    errorTagFeedItem(
                        tag = tag,
                        errorType = ErrorType.NoContent,
                    )
                )
            }
        } catch (e: ReaderPostFetchException) {
            updatedLoadedData.add(
                existingIndex,
                errorTagFeedItem(
                    tag = tag,
                    errorType = ErrorType.Default,
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

    private fun loadingTagsUiState(tags: List<ReaderTag>) {
        _uiStateFlow.update {
            UiState.Loaded(
                tags.map { tag ->
                    TagFeedItem(
                        tagChip = TagChip(
                            tag = tag,
                            onTagClick = ::onTagClick,
                        ),
                        postList = PostList.Loading,
                    )
                }
            )
        }
    }

    private fun loadedTagFeedItem(
        tag: ReaderTag,
        posts: ReaderPostList
    ) = TagFeedItem(
        tagChip = TagChip(
            tag = tag,
            onTagClick = ::onTagClick,
        ),
        postList = PostList.Loaded(
            posts.map {
                TagsFeedPostItem(
                    siteName = it.blogName,
                    postDateLine = dateTimeUtilsWrapper.javaDateToTimeSpan(
                        it.getDisplayDate(dateTimeUtilsWrapper)
                    ),
                    postTitle = it.title,
                    postExcerpt = it.excerpt,
                    postImageUrl = it.blogImageUrl,
                    postNumberOfLikesText = readerUtilsWrapper.getShortLikeLabelText(
                        numLikes = it.numLikes
                    ),
                    postNumberOfCommentsText = readerUtilsWrapper.getShortCommentLabelText(
                        numComments = it.numReplies
                    ),
                    isPostLiked = it.isLikedByCurrentUser,
                    onSiteClick = ::onSiteClick,
                    onPostImageClick = ::onPostImageClick,
                    onPostLikeClick = ::onPostLikeClick,
                    onPostMoreMenuClick = ::onPostMoreMenuClick,
                )
            }
        ),
    )

    private fun errorTagFeedItem(
        tag: ReaderTag,
        errorType: ErrorType,
    ): TagFeedItem =
        TagFeedItem(
            tagChip = TagChip(
                tag = tag,
                onTagClick = ::onTagClick
            ),
            postList = PostList.Error(
                type = errorType,
                onRetryClick = ::onRetryClick
            ),
        )

    private fun onOpenTagsListClick() {
        // TODO
    }

    private fun onTagClick() {
        // TODO
    }

    private fun onRetryClick() {
        // TODO
    }

    private fun onSiteClick() {
        // TODO
    }

    private fun onPostImageClick() {
        // TODO
    }

    private fun onPostLikeClick() {
        // TODO
    }

    private fun onPostMoreMenuClick() {
        // TODO
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
        val onTagClick: () -> Unit,
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
