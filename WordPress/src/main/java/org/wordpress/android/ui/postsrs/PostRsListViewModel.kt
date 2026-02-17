package org.wordpress.android.ui.postsrs

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
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
    private val postStore: PostStore,
) : ViewModel() {
    private val _tabStates =
        MutableStateFlow<Map<PostRsListTab, PostTabUiState>>(emptyMap())
    val tabStates: StateFlow<Map<PostRsListTab, PostTabUiState>> =
        _tabStates.asStateFlow()
    private val collections =
        mutableMapOf<PostRsListTab, ObservableMetadataCollection>()
    private val initializingTabs = mutableSetOf<PostRsListTab>()
    private val userRefreshingTabs = mutableSetOf<PostRsListTab>()

    private val _events = Channel<PostRsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * Looks up a post in the local FluxC database and emits an
     * [PostRsListEvent] to open the editor or show an error.
     * This FluxC dependency will be removed once the editor
     * supports loading posts via wordpress-rs.
     */
    @MainThread
    fun openPost(remotePostId: Long) {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _events.trySend(
                PostRsListEvent.ShowError(R.string.blog_not_found)
            )
            return
        }
        val post = postStore.getPostByRemotePostId(
            remotePostId, site
        )
        if (post == null) {
            _events.trySend(
                PostRsListEvent.ShowError(R.string.post_not_found)
            )
            return
        }
        _events.trySend(
            PostRsListEvent.EditPost(site, post)
        )
    }

    /** Emits a [PostRsListEvent.CreatePost] for the selected site. */
    @MainThread
    fun createNewPost() {
        val site = selectedSiteRepository.getSelectedSite()
            ?: return
        _events.trySend(PostRsListEvent.CreatePost(site))
    }

    /**
     * Initializes the observable collection for [tab] if it hasn't been
     * created yet. Creates the service, registers observers, then
     * triggers the first refresh.
     */
    @MainThread
    @Suppress("ReturnCount")
    fun initTab(tab: PostRsListTab) {
        if (collections.containsKey(tab) || initializingTabs.contains(tab)) {
            return
        }

        val site = selectedSiteRepository.getSelectedSite() ?: run {
            updateTabUiState(tab) {
                PostTabUiState(
                    error = resourceProvider.getString(
                        R.string.stats_todays_stats_no_site_selected
                    )
                )
            }
            return
        }

        initializingTabs.add(tab)

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val collection = createCollection(site, tab)
                collections[tab] = collection
                initializingTabs.remove(tab)
                registerObservers(tab, collection)
                refreshTab(tab)
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to init RS post list tab",
                    e
                )
                initializingTabs.remove(tab)
                updateTabUiState(tab) {
                    PostTabUiState(
                        error = e.message
                            ?: resourceProvider.getString(
                                R.string.error_generic
                            )
                    )
                }
            }
        }
    }

    private suspend fun createCollection(
        site: SiteModel,
        tab: PostRsListTab
    ): ObservableMetadataCollection = withContext(Dispatchers.IO) {
        val service = serviceProvider.getService(site)
        val filter = PostListFilter(
            status = tab.statuses,
            order = tab.order,
            orderby = WpApiParamPostsOrderBy.DATE
        )
        service.posts()
            .getObservablePostMetadataCollectionWithEditContext(
                endpointType = PostEndpointType.Posts,
                filter = filter,
                perPage = PAGE_SIZE.toUInt()
            )
    }

    private fun registerObservers(
        tab: PostRsListTab,
        collection: ObservableMetadataCollection
    ) {
        collection.addDataObserver {
            viewModelScope.launch { loadItemsForTab(tab) }
        }
        collection.addListInfoObserver {
            viewModelScope.launch { updateListInfoForTab(tab) }
        }
    }

    /**
     * Triggers a refresh for the given tab's collection. The PTR
     * indicator is only shown when [isUserRefresh] is true (i.e.
     * the user explicitly pulled to refresh).
     */
    @MainThread
    fun refreshTab(
        tab: PostRsListTab,
        isUserRefresh: Boolean = false
    ) {
        val collection = collections[tab] ?: return

        if (isUserRefresh) {
            userRefreshingTabs.add(tab)
            updateTabUiState(tab) {
                copy(isRefreshing = true, error = null)
            }
        } else {
            updateTabUiState(tab) { copy(error = null) }
        }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
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
                userRefreshingTabs.remove(tab)
                updateTabUiState(tab) {
                    copy(
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
    }

    /** Loads the next page of posts for [tab] if not already loading. */
    @MainThread
    fun loadMorePosts(tab: PostRsListTab) {
        val collection = collections[tab] ?: return
        val current = getTabUiState(tab)
        if (current.isLoadingMore || !current.canLoadMore) {
            return
        }

        updateTabUiState(tab) { copy(isLoadingMore = true) }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
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
                updateTabUiState(tab) {
                    copy(isLoadingMore = false)
                }
            }
        }
    }

    /**
     * Reads cached items from the collection and maps them to
     * [PostRsUiModel] instances for display.
     */
    private suspend fun loadItemsForTab(tab: PostRsListTab) {
        val collection = collections[tab] ?: return

        @Suppress("TooGenericExceptionCaught")
        try {
            val uiModels = withContext(Dispatchers.IO) {
                collection.loadItems().map { item ->
                    item.state.toUiModel(item.id)
                }
            }
            updateTabUiState(tab) {
                copy(
                    posts = uiModels,
                    isLoading = false,
                    error = null
                )
            }
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

        val listInfo = withContext(Dispatchers.IO) {
            collection.listInfo()
        }
        val morePages = listInfo?.hasMorePages ?: false
        val fetchingFirstPage =
            listInfo?.state == ListState.FETCHING_FIRST_PAGE
        val isUserRefresh = userRefreshingTabs.contains(tab)

        if (!fetchingFirstPage) {
            userRefreshingTabs.remove(tab)
        }

        updateTabUiState(tab) {
            copy(
                isRefreshing = isUserRefresh && fetchingFirstPage,
                isLoadingMore =
                    listInfo?.state == ListState.FETCHING_NEXT_PAGE,
                canLoadMore = morePages,
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
    }

    /** Returns the current UI state for [tab], or a default loading state. */
    private fun getTabUiState(tab: PostRsListTab): PostTabUiState {
        return _tabStates.value[tab]
            ?: PostTabUiState(isLoading = true)
    }

    /** Updates the UI state for [tab] by applying [update] to the current state. */
    private fun updateTabUiState(
        tab: PostRsListTab,
        update: PostTabUiState.() -> PostTabUiState
    ) {
        val current = getTabUiState(tab)
        _tabStates.value = _tabStates.value + (tab to current.update())
    }

    override fun onCleared() {
        super.onCleared()
        collections.values.forEach { it.close() }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

sealed interface PostRsListEvent {
    data class EditPost(
        val site: SiteModel,
        val post: PostModel
    ) : PostRsListEvent

    data class CreatePost(
        val site: SiteModel
    ) : PostRsListEvent

    data class ShowError(
        val messageResId: Int
    ) : PostRsListEvent
}
