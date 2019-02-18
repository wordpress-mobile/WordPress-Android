package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.throttle
import org.wordpress.android.viewmodel.ScopedViewModel

open class StatsListViewModel(
    defaultDispatcher: CoroutineDispatcher,
    private val statsUseCase: BaseListUseCase
) : ScopedViewModel(defaultDispatcher) {
    enum class StatsSection(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years);
    }

    val navigationTarget: LiveData<NavigationTarget> = statsUseCase.navigationTarget

    val uiModel: LiveData<UiModel> = statsUseCase.data.throttle(this, distinct = true)

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }

    fun onRetryClick(site: SiteModel) {
        launch {
            statsUseCase.refreshData(site, true)
        }
    }

    sealed class UiModel {
        data class Success(val data: List<StatsBlock>) : UiModel()
        class Error(val message: Int = R.string.stats_loading_error) : UiModel()
    }
}
