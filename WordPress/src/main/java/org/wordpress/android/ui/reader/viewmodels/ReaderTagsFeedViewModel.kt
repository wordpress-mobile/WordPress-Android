package org.wordpress.android.ui.reader.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderTagsFeedViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val readerPostRepository: ReaderPostRepository,
) : ScopedViewModel(bgDispatcher) {
    private val _uiStateFlow = MutableStateFlow(UiState(emptyMap()))
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow

    fun fetchAll(tags: List<ReaderTag>) {
        tags.forEach {
            fetchTag(it)
        }
    }

    fun fetchTag(tag: ReaderTag) {
        launch {
            _uiStateFlow.update {
                it.copy(tagStates = it.tagStates + (tag to FetchState.Loading))
            }

            try {
                val posts = readerPostRepository.fetchNewerPostsForTag(tag)
                _uiStateFlow.update {
                    it.copy(tagStates = it.tagStates + (tag to FetchState.Success(posts)))
                }
            } catch (e: Exception) {
                _uiStateFlow.update {
                    it.copy(tagStates = it.tagStates + (tag to FetchState.Error))
                }
            }
        }
    }

    data class UiState(
        val tagStates: Map<ReaderTag, FetchState>,
    )

    sealed class FetchState {
        data object Loading : FetchState()
        data object Error : FetchState()
        data class Success(val posts: ReaderPostList) : FetchState()
    }
}
