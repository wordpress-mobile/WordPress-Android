package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.util.Event
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel

open class StatsListViewModel(
    defaultDispatcher: CoroutineDispatcher,
    private val statsUseCase: BaseListUseCase,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val toUiModel: (useCaseModels: List<UseCaseModel>, showError: (Int) -> Unit) -> UiModel
) :
        ScopedViewModel(defaultDispatcher) {
    private val _data = statsUseCase.data

    enum class StatsSection(@StringRes val titleRes: Int) {
        INSIGHTS(R.string.stats_insights),
        DAYS(R.string.stats_timeframe_days),
        WEEKS(R.string.stats_timeframe_weeks),
        MONTHS(R.string.stats_timeframe_months),
        YEARS(R.string.stats_timeframe_years);
    }

    val navigationTarget: LiveData<NavigationTarget> = statsUseCase.navigationTarget
    private val mutableSnackbarMessage = MutableLiveData<SnackbarMessage>()
    val snackbarMessage: LiveData<SnackbarMessage> = mutableSnackbarMessage

    val uiModel: LiveData<UiModel> = _data.map { useCaseModels ->
        toUiModel(useCaseModels) { message ->
            mutableSnackbarMessage.value = SnackbarMessage(
                    message
            )
        }
    }

    override fun onCleared() {
        statsUseCase.onCleared()
        super.onCleared()
    }

    fun onRetryClick(site: SiteModel) {
        if (networkUtilsWrapper.isNetworkAvailable()) {
            launch {
                statsUseCase.refreshData(site, true)
            }
        } else {
            mutableSnackbarMessage.value = SnackbarMessage(R.string.no_network_title)
        }
    }

    sealed class UiModel {
        data class Success(val data: List<StatsBlock>) : UiModel()
        class Error(val message: Int = R.string.stats_loading_error) : UiModel()
    }

    data class SnackbarMessage(@StringRes val message: Int) : Event()
}
