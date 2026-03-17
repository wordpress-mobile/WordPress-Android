package org.wordpress.android.ui.newstats.subscribers.subscribersgraph

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.SubscribersGraphResult
import org.wordpress.android.ui.newstats.subscribers.BaseSubscribersCardViewModel
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SubscribersGraphViewModel @Inject constructor(
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
    statsRepository: StatsRepository,
    resourceProvider: ResourceProvider
) : BaseSubscribersCardViewModel<SubscribersGraphUiState>(
    selectedSiteRepository,
    accountStore,
    statsRepository,
    resourceProvider,
    SubscribersGraphUiState.Loading
) {
    private val _selectedTab =
        MutableStateFlow(SubscribersGraphTab.DAYS)
    val selectedTab: StateFlow<SubscribersGraphTab> =
        _selectedTab.asStateFlow()

    override val loadingState = SubscribersGraphUiState.Loading

    override fun errorState(
        message: String,
        isAuthError: Boolean
    ) = SubscribersGraphUiState.Error(message, isAuthError)

    fun onTabSelected(tab: SubscribersGraphTab) {
        if (tab == _selectedTab.value) return
        _selectedTab.value = tab
        resetLoadedSuccessfully()
        loadData()
    }

    override suspend fun loadDataInternal(siteId: Long) {
        val tab = _selectedTab.value
        val today = LocalDate.now()
        val dateStr = today.format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        when (
            val result = statsRepository
                .fetchSubscribersGraph(
                    siteId,
                    unit = tab.unit,
                    quantity = tab.quantity,
                    date = dateStr
                )
        ) {
            is SubscribersGraphResult.Success -> {
                markLoadedSuccessfully()
                val sorted = result.dataPoints
                    .sortedBy { it.date }
                updateState(
                    SubscribersGraphUiState.Loaded(
                        dataPoints = sorted.map {
                            GraphDataPoint(
                                label = formatLabel(
                                    it.date, tab
                                ),
                                count = it.count
                            )
                        }
                    )
                )
            }
            is SubscribersGraphResult.Error -> {
                updateState(
                    SubscribersGraphUiState.Error(
                        message = resourceProvider
                            .getString(result.messageResId),
                        isAuthError = result.isAuthError
                    )
                )
            }
        }
    }

    @Suppress("MagicNumber")
    private fun formatLabel(
        dateStr: String,
        tab: SubscribersGraphTab
    ): String {
        return try {
            val date = LocalDate.parse(dateStr)
            when (tab) {
                SubscribersGraphTab.DAYS,
                SubscribersGraphTab.WEEKS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "MMM d", Locale.getDefault()
                    )
                    date.format(fmt)
                }
                SubscribersGraphTab.MONTHS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "MMM", Locale.getDefault()
                    )
                    date.format(fmt)
                }
                SubscribersGraphTab.YEARS -> {
                    val fmt = DateTimeFormatter.ofPattern(
                        "yyyy", Locale.getDefault()
                    )
                    date.format(fmt)
                }
            }
        } catch (_: Exception) {
            dateStr
        }
    }
}
