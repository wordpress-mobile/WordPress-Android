package org.wordpress.android.ui.newstats.subscribers.emails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.EmailsStatsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

private const val CARD_MAX_ITEMS = 5
private const val DETAIL_MAX_ITEMS = 100

@HiltViewModel
class EmailsCardViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<
        EmailsCardUiState>(EmailsCardUiState.Loading)
    val uiState: StateFlow<EmailsCardUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var isLoading = false
    private var isLoadedSuccessfully = false
    private var allItems: List<EmailListItem> =
        emptyList()

    fun loadDataIfNeeded() {
        if (isLoadedSuccessfully || isLoading) return
        isLoading = true
        loadData()
    }

    fun refresh() {
        val site =
            selectedSiteRepository.getSelectedSite()
                ?: return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                loadDataInternal(site.siteId)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadData() {
        val site =
            selectedSiteRepository.getSelectedSite()
        if (site == null) {
            isLoading = false
            _uiState.value = EmailsCardUiState
                .Error(message = "No site selected")
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            isLoading = false
            _uiState.value = EmailsCardUiState
                .Error(message = "Not authenticated")
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = EmailsCardUiState.Loading

        viewModelScope.launch {
            try {
                loadDataInternal(site.siteId)
            } finally {
                isLoading = false
            }
        }
    }

    fun getDetailData(): List<EmailListItem> = allItems

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(siteId: Long) {
        try {
            when (
                val result = statsRepository
                    .fetchEmailsSummary(
                        siteId, DETAIL_MAX_ITEMS
                    )
            ) {
                is EmailsStatsResult.Success -> {
                    isLoadedSuccessfully = true
                    allItems = result.items.map {
                        EmailListItem(
                            title = it.title,
                            opens = it.opens,
                            clicks = it.clicks
                        )
                    }
                    _uiState.value =
                        EmailsCardUiState.Loaded(
                            items = allItems.take(
                                CARD_MAX_ITEMS
                            )
                        )
                }
                is EmailsStatsResult.Error -> {
                    _uiState.value =
                        EmailsCardUiState.Error(
                            message = resourceProvider
                                .getString(
                                    result.messageResId
                                ),
                            isAuthError =
                                result.isAuthError
                        )
                }
            }
        } catch (e: Exception) {
            _uiState.value = EmailsCardUiState.Error(
                message = e.message ?: "Unknown error"
            )
        }
    }
}
