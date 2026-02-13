package org.wordpress.android.ui.postsrs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.WpSelfHostedServiceProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
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
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    private val tabStates =
        mutableMapOf<PostRsListTab, MutableStateFlow<PostTabUiState>>()
    private val collections =
        mutableMapOf<PostRsListTab, ObservableMetadataCollection>()
    private val initializingTabs = mutableSetOf<PostRsListTab>()

    /** Returns an observable [StateFlow] of UI state for the given tab. */
    fun getTabState(tab: PostRsListTab): StateFlow<PostTabUiState> {
        return tabStates.getOrPut(tab) {
            MutableStateFlow(PostTabUiState(isLoading = true))
        }.asStateFlow()
    }

    /**
     * Initializes the observable collection for [tab] if it hasn't been
     * created yet. Creates the [WpSelfHostedService], builds a
     * [PostListFilter], registers data and list-info observers, then
     * triggers the first refresh.
     */
    fun initTab(tab: PostRsListTab) {
        if (collections.containsKey(tab)) return
        if (initializingTabs.contains(tab)) return

        val site = selectedSiteRepository.getSelectedSite() ?: run {
            getOrCreateStateFlow(tab).value = PostTabUiState(
                error = resourceProvider.getString(
                    R.string.stats_todays_stats_no_site_selected
                )
            )
            return
        }

        initializingTabs.add(tab)

        viewModelScope.launch {
            try {
                val collection = withContext(Dispatchers.IO) {
                    val service = serviceProvider.getService(site)
                    val postService = service.posts()
                    val filter = PostListFilter(
                        status = tab.statuses,
                        order = tab.order,
                        orderby = WpApiParamPostsOrderBy.DATE
                    )
                    postService
                        .getObservablePostMetadataCollectionWithEditContext(
                            endpointType = PostEndpointType.Posts,
                            filter = filter,
                            perPage = PAGE_SIZE.toUInt()
                        )
                }

                collections[tab] = collection
                initializingTabs.remove(tab)

                collection.addDataObserver {
                    viewModelScope.launch {
                        loadItemsForTab(tab)
                    }
                }

                collection.addListInfoObserver {
                    viewModelScope.launch {
                        updateListInfoForTab(tab)
                    }
                }

                refreshTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to init RS post list tab",
                    e
                )
                initializingTabs.remove(tab)
                getOrCreateStateFlow(tab).value = PostTabUiState(
                    error = e.message
                        ?: resourceProvider.getString(
                            R.string.error_generic
                        )
                )
            }
        }
    }

    /** Triggers a pull-to-refresh for the given tab's collection. */
    fun refreshTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)
        state.value = state.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    collection.refresh()
                }
                loadItemsForTab(tab)
                updateListInfoForTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to refresh tab $tab",
                    e
                )
                state.value = state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message
                        ?: resourceProvider.getString(
                            R.string.error_generic
                        )
                )
            }
        }
    }

    /** Loads the next page of posts for [tab] if not already loading. */
    fun loadMorePosts(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)
        if (state.value.isLoadingMore || !state.value.canLoadMore) {
            return
        }

        state.value = state.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    collection.loadNextPage()
                }
                loadItemsForTab(tab)
                updateListInfoForTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to load more for tab $tab",
                    e
                )
                state.value = state.value.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * Reads cached items from the collection and maps them to
     * [PostRsUiModel] instances for display.
     */
    private suspend fun loadItemsForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)

        try {
            val uiModels = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toUiModel(item.id)
                }
            }
            state.value = state.value.copy(
                posts = uiModels,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.POSTS,
                "Failed to load items for tab $tab",
                e
            )
        }
    }

    /**
     * Reads pagination and sync state from the collection's list info
     * and updates the tab's UI state accordingly.
     */
    private suspend fun updateListInfoForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val state = getOrCreateStateFlow(tab)

        val listInfo = withContext(Dispatchers.IO) {
            collection.listInfo()
        }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage =
            listInfo?.state == ListState.FETCHING_FIRST_PAGE

        state.value = state.value.copy(
            isRefreshing = fetchingFirstPage,
            isLoadingMore =
                listInfo?.state == ListState.FETCHING_NEXT_PAGE,
            canLoadMore = morePages,
            isLoading =
                fetchingFirstPage && state.value.posts.isEmpty(),
            error = if (listInfo?.state == ListState.ERROR) {
                listInfo.errorMessage
                    ?: resourceProvider.getString(
                        R.string.error_generic
                    )
            } else {
                null
            }
        )
    }

    /** Returns the mutable state flow for [tab], creating one if needed. */
    private fun getOrCreateStateFlow(
        tab: PostRsListTab
    ): MutableStateFlow<PostTabUiState> {
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
