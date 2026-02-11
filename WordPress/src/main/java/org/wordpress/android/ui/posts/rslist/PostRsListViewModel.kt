package org.wordpress.android.ui.posts.rslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.rslist.data.PostRsRestClient
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PostRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val postRsRestClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val tabStates: Map<PostRsListTab, MutableStateFlow<PostTabUiState>> =
        PostRsListTab.entries.associateWith {
            MutableStateFlow(PostTabUiState())
        }

    private val _selectedTab =
        MutableStateFlow(PostRsListTab.PUBLISHED)
    val selectedTab: StateFlow<PostRsListTab> =
        _selectedTab.asStateFlow()

    private val _uiEvent = MutableStateFlow<PostRsListUiEvent?>(null)
    val uiEvent: StateFlow<PostRsListUiEvent?> = _uiEvent.asStateFlow()

    private val paginationMutex = Mutex()

    // Track loaded post count per tab for offset pagination
    private val loadedCounts = mutableMapOf<PostRsListTab, Int>()

    fun tabState(tab: PostRsListTab): StateFlow<PostTabUiState> =
        tabStates.getValue(tab).asStateFlow()

    fun onTabSelected(tab: PostRsListTab) {
        _selectedTab.value = tab
        val state = tabStates.getValue(tab).value
        if (state.posts.isEmpty() && !state.isLoading) {
            loadPosts(tab)
        }
    }

    private fun loadPosts(tab: PostRsListTab) {
        loadPostsInternal(tab, isRefresh = false)
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

                withContext(ioDispatcher) {
                    val result = postRsRestClient.fetchPosts(
                        site = site,
                        statuses = tab.statuses,
                        offset = offset,
                        order = tab.order
                    )

                    withContext(mainDispatcher) {
                        when (result) {
                            is PostRsRestClient.PostRsListResult
                            .Success -> {
                                val newModels = result.posts
                                    .map { it.toUiModel() }
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
                                _uiEvent.value =
                                    PostRsListUiEvent.ShowError(
                                        result.message
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onPostClicked(remotePostId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _uiEvent.value = PostRsListUiEvent.ShowError(
                resourceProvider.getString(
                    R.string.menu_error_no_site_selected
                )
            )
            return
        }
        _uiEvent.value = PostRsListUiEvent.OpenPost(
            remotePostId = remotePostId
        )
    }

    fun consumeEvent() {
        _uiEvent.value = null
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

            withContext(ioDispatcher) {
                val result = postRsRestClient.fetchPosts(
                    site = site,
                    statuses = tab.statuses,
                    offset = 0,
                    order = tab.order
                )

                withContext(mainDispatcher) {
                    when (result) {
                        is PostRsRestClient.PostRsListResult
                        .Success -> {
                            val uiModels = result.posts
                                .map { it.toUiModel() }
                            loadedCounts[tab] = result.posts.size
                            flow.value = flow.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                canLoadMore = result.canLoadMore,
                                posts = uiModels
                            )
                        }
                        is PostRsRestClient.PostRsListResult
                        .Error -> {
                            flow.value = flow.value.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }
}
