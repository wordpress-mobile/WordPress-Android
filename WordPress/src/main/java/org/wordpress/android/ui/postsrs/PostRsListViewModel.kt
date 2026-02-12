package org.wordpress.android.ui.postsrs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.WpSelfHostedServiceProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.cache.kotlin.ObservableMetadataCollection
import rs.wordpress.cache.kotlin.getObservablePostMetadataCollectionWithEditContext
import rs.wordpress.cache.kotlin.hasMorePages
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.WpApiParamPostsOrderBy
import uniffi.wp_mobile.PostListFilter
import uniffi.wp_mobile_cache.ListState
import javax.inject.Inject

@HiltViewModel
class PostRsListViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val serviceProvider: WpSelfHostedServiceProvider,
) : ViewModel() {
    private val tabStates = mutableMapOf<PostRsListTab, MutableStateFlow<PostTabUiState>>()
    private val collections = mutableMapOf<PostRsListTab, ObservableMetadataCollection>()

    private val _events = MutableSharedFlow<PostRsListUiEvent>()
    val events = _events.asSharedFlow()

    fun getTabState(tab: PostRsListTab): StateFlow<PostTabUiState> {
        return tabStates.getOrPut(tab) {
            MutableStateFlow(PostTabUiState(isLoading = true))
        }.asStateFlow()
    }

    fun initTab(tab: PostRsListTab) {
        if (collections.containsKey(tab)) return

        val site = selectedSiteRepository.getSelectedSite() ?: run {
            getOrCreateStateFlow(tab).value = PostTabUiState(
                error = "No site selected"
            )
            return
        }

        try {
            val service = serviceProvider.getService(site)
            val postService = service.posts()
            val filter = PostListFilter(
                status = tab.statuses,
                order = tab.order,
                orderby = WpApiParamPostsOrderBy.DATE
            )

            val collection =
                postService.getObservablePostMetadataCollectionWithEditContext(
                    endpointType = PostEndpointType.Posts,
                    filter = filter,
                    perPage = PAGE_SIZE.toUInt()
                )

            collections[tab] = collection

            collection.addDataObserver {
                viewModelScope.launch { loadItemsForTab(tab) }
            }

            collection.addListInfoObserver {
                viewModelScope.launch { updateListInfoForTab(tab) }
            }

            refreshTab(tab)
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Failed to init RS post list tab", e)
            getOrCreateStateFlow(tab).value = PostTabUiState(
                error = e.message ?: "Failed to initialize"
            )
        }
    }

    fun refreshTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)
        state.value = state.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            try {
                collection.refresh()
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to refresh tab $tab", e)
                state.value = state.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Refresh failed"
                )
            }
        }
    }

    fun loadMorePosts(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)
        if (state.value.isLoadingMore || !state.value.canLoadMore) return

        state.value = state.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                collection.loadNextPage()
            } catch (e: Exception) {
                AppLog.e(AppLog.T.POSTS, "Failed to load more for tab $tab", e)
                state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    fun refreshAllTabs() {
        PostRsListTab.entries.forEach { tab ->
            if (collections.containsKey(tab)) {
                refreshTab(tab)
            }
        }
    }

    private suspend fun loadItemsForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)

        try {
            val items = collection.loadItems()
            val uiModels = items.map { item ->
                item.state.toUiModel(item.id)
            }
            state.value = state.value.copy(
                posts = uiModels,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            AppLog.e(AppLog.T.POSTS, "Failed to load items for tab $tab", e)
        }
    }

    private fun updateListInfoForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)

        val listInfo = collection.listInfo()
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage =
            listInfo?.state == ListState.FETCHING_FIRST_PAGE

        state.value = state.value.copy(
            isRefreshing = fetchingFirstPage,
            isLoadingMore = listInfo?.state == ListState.FETCHING_NEXT_PAGE,
            canLoadMore = morePages,
            isLoading = fetchingFirstPage && state.value.posts.isEmpty(),
            error = if (listInfo?.state == ListState.ERROR) {
                listInfo.errorMessage ?: "Unknown error"
            } else {
                null
            }
        )
    }

    private fun getOrCreateStateFlow(tab: PostRsListTab): MutableStateFlow<PostTabUiState> {
        return tabStates.getOrPut(tab) {
            MutableStateFlow(PostTabUiState(isLoading = true))
        }
    }

    override fun onCleared() {
        super.onCleared()
        collections.values.forEach { it.close() }
        collections.clear()
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
