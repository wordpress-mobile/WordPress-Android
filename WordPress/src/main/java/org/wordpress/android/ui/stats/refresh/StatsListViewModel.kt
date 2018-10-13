package org.wordpress.android.ui.stats.refresh

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.stats.refresh.InsightsItem.Type.NOT_IMPLEMENTED
import org.wordpress.android.ui.stats.refresh.InsightsUiState.ListStatus.DONE
import org.wordpress.android.ui.stats.refresh.InsightsUiState.ListStatus.FETCHING
import javax.inject.Inject
import javax.inject.Named

class StatsListViewModel
@Inject constructor(
    private val insightsDomain: InsightsDomain,
    @Named(UI_SCOPE) private val uiScope: CoroutineScope
) : ViewModel() {
    enum class StatsListType(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months);
    }

    private val mutableData: MutableLiveData<InsightsUiState> = MutableLiveData()
    val data: LiveData<InsightsUiState> = mutableData
    fun start() {
        if (mutableData.value == null) {
            mutableData.value = InsightsUiState(status = FETCHING)
        }
        uiScope.launch {
            val loadedData = insightsDomain.loadInsightItems()
            mutableData.value = InsightsUiState(loadedData, DONE)
        }
    }
}

data class NotImplemented(val text: String) : InsightsItem(NOT_IMPLEMENTED)

data class InsightsUiState(val data: List<InsightsItem> = listOf(), val status: ListStatus) {
    enum class ListStatus {
        DONE,
        ERROR,
        FETCHING
    }
}
