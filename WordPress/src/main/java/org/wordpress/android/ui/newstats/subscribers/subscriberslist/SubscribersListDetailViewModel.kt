package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersListResult
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal const val SUBSCRIBERS_DETAIL_PAGE_SIZE = 20

@HiltViewModel
class SubscribersListDetailViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val contextProvider: ContextProvider
) : ViewModel() {
    private val _items = MutableStateFlow<List<SubscriberListItem>>(
        emptyList()
    )
    val items: StateFlow<List<SubscriberListItem>> =
        _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> =
        _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> =
        _canLoadMore.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> =
        _hasError.asStateFlow()

    private var currentPage = 0
    private val paginationMutex = Mutex()

    fun loadInitialPage() {
        viewModelScope.launch {
            paginationMutex.withLock {
                if (_items.value.isNotEmpty()) return@launch
                currentPage = 1
                _isLoading.value = true
                _hasError.value = false
                _canLoadMore.value = true
                fetchPage(currentPage, isInitial = true)
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            paginationMutex.withLock {
                if (!_canLoadMore.value ||
                    _isLoadingMore.value
                ) return@launch
                _isLoadingMore.value = true
                currentPage++
                fetchPage(currentPage, isInitial = false)
                _isLoadingMore.value = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchPage(
        page: Int,
        isInitial: Boolean
    ) {
        val siteId = selectedSiteRepository
            .getSelectedSite()?.siteId ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return
        statsRepository.init(accessToken)

        val resources =
            contextProvider.getContext().resources
        try {
            val result = statsRepository.fetchSubscribersList(
                siteId = siteId,
                perPage = SUBSCRIBERS_DETAIL_PAGE_SIZE,
                page = page
            )
            when (result) {
                is SubscribersListResult.Success -> {
                    val newItems = result.subscribers.map {
                        SubscriberListItem(
                            displayName = it.displayName,
                            subscribedSince =
                                it.subscribedSince,
                            formattedDate =
                                formatSubscriberDate(
                                    it.subscribedSince,
                                    resources
                                )
                        )
                    }
                    if (isInitial) {
                        _items.value = newItems
                    } else {
                        _items.value =
                            _items.value + newItems
                    }
                    _canLoadMore.value =
                        newItems.size ==
                            SUBSCRIBERS_DETAIL_PAGE_SIZE
                }
                is SubscribersListResult.Error ->
                    handleFetchError(isInitial)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching subscribers detail",
                e
            )
            handleFetchError(isInitial)
        }
    }

    private fun handleFetchError(isInitial: Boolean) {
        if (isInitial) {
            _hasError.value = true
            _canLoadMore.value = false
        } else {
            currentPage--
        }
    }
}
