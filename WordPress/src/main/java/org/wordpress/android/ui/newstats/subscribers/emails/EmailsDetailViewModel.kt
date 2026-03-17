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
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val EMAILS_MAX_ITEMS = 25

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

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> =
        _hasError.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun loadData() {
        viewModelScope.launch {
            if (_items.value.isNotEmpty()) return@launch
            _isLoading.value = true
            _hasError.value = false

            val siteId = selectedSiteRepository
                .getSelectedSite()?.siteId
            val accessToken = accountStore.accessToken
            if (siteId == null ||
                accessToken.isNullOrEmpty()
            ) {
                _hasError.value = true
                _isLoading.value = false
                return@launch
            }
            statsRepository.init(accessToken)

            try {
                val result =
                    statsRepository.fetchEmailsSummary(
                        siteId = siteId,
                        quantity = EMAILS_MAX_ITEMS
                    )
                when (result) {
                    is EmailsStatsResult.Success -> {
                        _items.value = result.items.map {
                            EmailListItem(
                                title = it.title,
                                opens = it.opens,
                                clicks = it.clicks
                            )
                        }
                    }
                    is EmailsStatsResult.Error -> {
                        _hasError.value = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.STATS,
                    "Error fetching emails detail",
                    e
                )
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
