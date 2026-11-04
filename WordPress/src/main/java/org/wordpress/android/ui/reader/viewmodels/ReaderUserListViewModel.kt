package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.ReaderUserTable
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

data class ReaderLiker(
    val userId: Long,
    val name: String,
    val urlDomain: String?,
    val avatarUrl: String,
    /** 0 when the liker has no blog to preview */
    val blogId: Long
)

data class ReaderUserListUiState(
    val isLoaded: Boolean = false,
    val numLikes: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val likers: List<ReaderLiker> = emptyList()
)

/**
 * The users who like a Reader post
 */
@HiltViewModel
class ReaderUserListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val blogId = savedStateHandle.get<Long>(ReaderConstants.ARG_BLOG_ID) ?: 0L
    private val postId = savedStateHandle.get<Long>(ReaderConstants.ARG_POST_ID) ?: 0L

    private val _uiState = MutableStateFlow(ReaderUserListUiState())
    val uiState: StateFlow<ReaderUserListUiState> = _uiState.asStateFlow()

    init {
        launch {
            _uiState.value = withContext(bgDispatcher) { loadUiState() }
        }
    }

    private fun loadUiState(): ReaderUserListUiState {
        val users = ReaderUserTable.getUsersWhoLikePost(blogId, postId, ReaderConstants.READER_MAX_USERS_TO_DISPLAY)
        return ReaderUserListUiState(
            isLoaded = true,
            numLikes = ReaderPostTable.getNumLikesForPost(blogId, postId),
            isLikedByCurrentUser = ReaderPostTable.isPostLikedByCurrentUser(blogId, postId),
            likers = users.map { user ->
                ReaderLiker(
                    userId = user.userId,
                    name = user.displayName,
                    urlDomain = user.urlDomain.ifEmpty { null },
                    avatarUrl = user.avatarUrl,
                    // as before, only a liker with a url can be opened
                    blogId = if (user.hasUrl() && user.hasBlogId()) user.blogId else 0L
                )
            }
        )
    }
}
