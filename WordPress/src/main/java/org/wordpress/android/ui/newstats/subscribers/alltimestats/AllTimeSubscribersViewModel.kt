package org.wordpress.android.ui.newstats.subscribers.alltimestats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersAllTimeResult
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class AllTimeSubscribersViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<AllTimeSubscribersUiState>(
        AllTimeSubscribersUiState.Loading
    )
    val uiState: StateFlow<AllTimeSubscribersUiState> =
        _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var isLoading = false
    private var isLoadedSuccessfully = false

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
            _uiState.value = AllTimeSubscribersUiState
                .Error(message = "No site selected")
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            isLoading = false
            _uiState.value = AllTimeSubscribersUiState
                .Error(message = "Not authenticated")
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = AllTimeSubscribersUiState.Loading

        viewModelScope.launch {
            try {
                loadDataInternal(site.siteId)
            } finally {
                isLoading = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadDataInternal(siteId: Long) {
        try {
            when (
                val result = statsRepository
                    .fetchSubscribersAllTime(siteId)
            ) {
                is SubscribersAllTimeResult.Success -> {
                    isLoadedSuccessfully = true
                    _uiState.value =
                        AllTimeSubscribersUiState.Loaded(
                            currentCount =
                                result.currentCount,
                            count30DaysAgo =
                                result.count30DaysAgo,
                            count60DaysAgo =
                                result.count60DaysAgo,
                            count90DaysAgo =
                                result.count90DaysAgo
                        )
                }
                is SubscribersAllTimeResult.Error -> {
                    _uiState.value =
                        AllTimeSubscribersUiState.Error(
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
            _uiState.value =
                AllTimeSubscribersUiState.Error(
                    message = e.message
                        ?: "Unknown error"
                )
        }
    }
}
