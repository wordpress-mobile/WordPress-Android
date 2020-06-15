package org.wordpress.android.ui.reader.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderDiscoverViewModel @Inject constructor(
    private val readerPostRepository: ReaderPostRepository,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MediatorLiveData<DiscoverUiState>()
    val uiState: LiveData<DiscoverUiState> = _uiState

    fun start() {
        if (isStarted) return
        isStarted = true

        init()
        loadPosts()
    }

    private fun init() {
        // Start with loading state
        _uiState.value = LoadingUiState

        // Listen to changes to the discover feed
        _uiState.addSource(readerPostRepository.discoveryFeed) { posts ->
            _uiState.value = ContentUiState(posts)
        }
    }

    private fun loadPosts() {
        // TODO we'll remove this method when the repositories start managing the requests automatically
        launch(bgDispatcher) {
            readerPostRepository.getDiscoveryFeed()
        }
    }

    sealed class DiscoverUiState {
        data class ContentUiState(val posts: ReaderPostList) : DiscoverUiState()
        object LoadingUiState : DiscoverUiState()
        object ErrorUiState : DiscoverUiState()
    }
}
