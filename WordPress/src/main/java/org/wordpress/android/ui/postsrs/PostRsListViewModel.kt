package org.wordpress.android.ui.postsrs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PostRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val postRsRestClient: PostRsRestClient,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    private val dispatcher: Dispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    init {
        dispatcher.register(this)
        loadPostsInternal(
            PostRsListTab.PUBLISHED, isRefresh = false
        )
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    /**
     * Event fired by FluxC when a post is uploaded. We should remove this once
     * the new post editor using wordpress-rs is available.
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (!event.isError) {
            refreshPosts(_selectedTab.value)
        }
    }

    private val tabStates: Map<PostRsListTab, MutableStateFlow<PostTabUiState>> =
        PostRsListTab.entries.associateWith {
            MutableStateFlow(PostTabUiState())
        }

    private val _selectedTab =
        MutableStateFlow(PostRsListTab.PUBLISHED)
    val selectedTab: StateFlow<PostRsListTab> =
        _selectedTab.asStateFlow()

    private val _uiEvent = Channel<PostRsListUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<PostRsListUiEvent> = _uiEvent.receiveAsFlow()

    private val paginationMutex = Mutex()
    private var tabSelectionJob: Job? = null

    // Track loaded post count per tab for offset pagination
    private val loadedCounts = mutableMapOf<PostRsListTab, Int>()

    fun tabState(tab: PostRsListTab): StateFlow<PostTabUiState> =
        tabStates.getValue(tab).asStateFlow()

    fun onTabSelected(tab: PostRsListTab) {
        _selectedTab.value = tab
        tabSelectionJob?.cancel()
        tabSelectionJob = viewModelScope.launch {
            delay(TAB_DEBOUNCE_MS)
            val state = tabStates.getValue(tab).value
            if (state.posts.isEmpty() && !state.isLoading) {
                loadPostsInternal(tab, isRefresh = false)
            }
        }
    }

    fun refreshPosts(tab: PostRsListTab) {
        loadPostsInternal(tab, isRefresh = true)
    }

    fun loadMorePosts(tab: PostRsListTab) {
        viewModelScope.launch {
            paginationMutex.withLock {
                val flow = tabStates.getValue(tab)
                val current = flow.value
                if (current.isLoading
                    || current.isLoadingMore
                    || !current.canLoadMore
                ) return@launch

                flow.value = current.copy(isLoadingMore = true)

                val site =
                    selectedSiteRepository.getSelectedSite()
                if (site == null) {
                    flow.value = current.copy(
                        isLoadingMore = false
                    )
                    return@launch
                }

                val offset = loadedCounts[tab] ?: 0

                val result = withContext(ioDispatcher) {
                    postRsRestClient.fetchPosts(
                        site = site,
                        statuses = tab.statuses,
                        offset = offset,
                        order = tab.order
                    )
                }

                when (result) {
                    is PostRsRestClient.PostRsListResult
                    .Success -> {
                        val newModels = result.posts
                            .map { it.toUiModel(dateTimeUtilsWrapper, resourceProvider) }
                        loadedCounts[tab] =
                            offset + result.posts.size
                        flow.value = flow.value.copy(
                            isLoadingMore = false,
                            canLoadMore =
                                result.canLoadMore,
                            posts = flow.value.posts +
                                newModels
                        )
                    }
                    is PostRsRestClient.PostRsListResult
                    .Error -> {
                        flow.value = flow.value.copy(
                            isLoadingMore = false
                        )
                        _uiEvent.trySend(
                            PostRsListUiEvent.ShowError(
                                result.message
                            )
                        )
                    }
                }
            }
        }
    }

    fun onPostClicked(remotePostId: Long) {
        _uiEvent.trySend(
            PostRsListUiEvent.OpenPost(
                remotePostId = remotePostId
            )
        )
    }

    private fun loadPostsInternal(
        tab: PostRsListTab,
        isRefresh: Boolean
    ) {
        viewModelScope.launch {
            val flow = tabStates.getValue(tab)
            flow.value = flow.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                error = null
            )

            val site = selectedSiteRepository.getSelectedSite()
            if (site == null) {
                flow.value = flow.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = resourceProvider.getString(
                        R.string.menu_error_no_site_selected
                    )
                )
                return@launch
            }

            val result = withContext(ioDispatcher) {
                postRsRestClient.fetchPosts(
                    site = site,
                    statuses = tab.statuses,
                    offset = 0,
                    order = tab.order
                )
            }

            when (result) {
                is PostRsRestClient.PostRsListResult.Success -> {
                    val uiModels = result.posts
                        .map { it.toUiModel(dateTimeUtilsWrapper, resourceProvider) }
                    loadedCounts[tab] = result.posts.size
                    flow.value = flow.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = result.canLoadMore,
                        posts = uiModels
                    )
                }
                is PostRsRestClient.PostRsListResult.Error -> {
                    flow.value = flow.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.message
                    )
                }
            }
        }
    }

    companion object {
        // Delay before fetching posts after a tab switch to avoid redundant
        // requests during rapid swiping
        private const val TAB_DEBOUNCE_MS = 300L
    }
}
