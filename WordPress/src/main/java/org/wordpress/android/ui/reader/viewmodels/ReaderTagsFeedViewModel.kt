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
import org.wordpress.android.ui.reader.views.compose.tagsfeed.TagsFeedPostItem
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderTagsFeedViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val readerPostRepository: ReaderPostRepository,
) : ScopedViewModel(bgDispatcher) {
    private val _uiStateFlow = MutableStateFlow(UiState.Initial)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow

    /**
     * Fetch multiple tag posts in parallel. Each tag load causes a new state to be emitted, so multiple emissions of
     * [uiStateFlow] are expected when calling this method for each tag, since each can go through the following
     * [FetchState]s: [FetchState.Loading], [FetchState.Success], [FetchState.Error].
     */
    fun fetchAll(tags: List<ReaderTag>) {
        tags.forEach {
            fetchTag(it)
        }
    }

    /**
     * Fetch posts for a single tag. This method will emit a new state to [uiStateFlow] for different [FetchState]s:
     * [FetchState.Loading], [FetchState.Success], [FetchState.Error], but only for the tag being fetched.
     *
     * Can be used for retrying a failed fetch, for instance.
     */
    fun fetchTag(tag: ReaderTag) {
        launch {
//            _uiStateFlow.update {
//                it.copy(tagStates = it.tagStates + (tag to FetchState.Loading))
//            }
//
//            try {
//                val posts = readerPostRepository.fetchNewerPostsForTag(tag)
//                _uiStateFlow.update {
//                    it.copy(tagStates = it.tagStates + (tag to FetchState.Success(posts)))
//                }
//            } catch (e: ReaderPostFetchException) {
//                _uiStateFlow.update {
//                    it.copy(tagStates = it.tagStates + (tag to FetchState.Error(e)))
//                }
//            }
        }
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
        val onTagClicked: () -> Unit,
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
        data object Loading : ErrorType

        data object NoContent : ErrorType
    }
}
