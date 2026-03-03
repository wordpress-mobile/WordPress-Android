package org.wordpress.android.ui.newstats.subscribers.emails

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
import org.wordpress.android.ui.newstats.repository.EmailsStatsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import javax.inject.Inject

internal const val EMAILS_DETAIL_PAGE_SIZE = 20

@HiltViewModel
class EmailsDetailViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val _items = MutableStateFlow<List<EmailListItem>>(
        emptyList()
    )
    val items: StateFlow<List<EmailListItem>> =
        _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> =
        _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> =
        _canLoadMore.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> =
        _hasError.asStateFlow()

    private var currentQuantity = 0
    private val paginationMutex = Mutex()

    fun loadInitialPage() {
        viewModelScope.launch {
            paginationMutex.withLock {
                if (_items.value.isNotEmpty()) return@launch
                currentQuantity = EMAILS_DETAIL_PAGE_SIZE
                _isLoading.value = true
                _hasError.value = false
                _canLoadMore.value = true
                fetchEmails(currentQuantity, isInitial = true)
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
                currentQuantity += EMAILS_DETAIL_PAGE_SIZE
                fetchEmails(
                    currentQuantity, isInitial = false
                )
                _isLoadingMore.value = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchEmails(
        quantity: Int,
        isInitial: Boolean
    ) {
        val siteId = selectedSiteRepository
            .getSelectedSite()?.siteId ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return
        statsRepository.init(accessToken)

        try {
            val result = statsRepository.fetchEmailsSummary(
                siteId = siteId,
                quantity = quantity
            )
            when (result) {
                is EmailsStatsResult.Success -> {
                    val newItems = result.items.map {
                        EmailListItem(
                            title = it.title,
                            opens = it.opens,
                            clicks = it.clicks
                        )
                    }
                    _items.value = newItems
                    _canLoadMore.value =
                        newItems.size == quantity
                }
                is EmailsStatsResult.Error -> {
                    if (isInitial) {
                        _hasError.value = true
                        _canLoadMore.value = false
                    } else {
                        currentQuantity -= EMAILS_DETAIL_PAGE_SIZE
                    }
                }
            }
        } catch (_: Exception) {
            if (isInitial) {
                _hasError.value = true
                _canLoadMore.value = false
            } else {
                currentQuantity -= EMAILS_DETAIL_PAGE_SIZE
            }
        }
    }
}
