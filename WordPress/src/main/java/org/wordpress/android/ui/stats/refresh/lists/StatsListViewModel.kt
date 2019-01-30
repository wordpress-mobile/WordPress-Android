package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.StringRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Error
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.Event
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel

open class StatsListViewModel(
    defaultDispatcher: CoroutineDispatcher,
    private val statsUseCase: BaseListUseCase,
    private val networkUtilsWrapper: NetworkUtilsWrapper
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

    val uiModel: LiveData<UiModel<out List<StatsBlock>>> = _data.map {
        val allFailing = it.fold(true) { acc, useCaseModel ->
            acc && useCaseModel.state == ERROR
        }
        val allFailingWithoutData = it.fold(true) { acc, useCaseModel ->
            acc && useCaseModel.state == ERROR && useCaseModel.data == null
        }
        if (!allFailing && !allFailingWithoutData) {
            UiModel.Success(it.map { useCaseModel ->
                when(useCaseModel.state) {
                    SUCCESS -> StatsBlock.Success(useCaseModel.type, useCaseModel.data ?: listOf())
                    ERROR -> StatsBlock.Error(
                            useCaseModel.type,
                            useCaseModel.stateData ?: useCaseModel.data ?: listOf()
                    )
                    LOADING -> StatsBlock.Loading(
                            useCaseModel.type,
                            useCaseModel.stateData ?: useCaseModel.data ?: listOf()
                    )
                    EMPTY -> StatsBlock.EmptyBlock(
                            useCaseModel.type,
                            useCaseModel.stateData ?: useCaseModel.data ?: listOf()
                    )
                }
            })
        } else if (!allFailingWithoutData) {
            mutableSnackbarMessage.value = SnackbarMessage(getErrorMessage())
            UiModel.Success(it.map { useCaseModel ->
                when(useCaseModel.state) {
                    SUCCESS, LOADING, EMPTY, ERROR -> Error(
                            useCaseModel.type,
                            useCaseModel.data ?: useCaseModel.stateData ?: listOf()
                    )
                }
            })
        } else {
            UiModel.Error<List<StatsBlock>>(getErrorMessage())
        }
    }

    private fun getErrorMessage(): Int {
        return if (networkUtilsWrapper.isNetworkAvailable()) {
            string.stats_loading_error
        } else {
            string.no_network_title
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

    sealed class UiModel<T> {
        data class Success<T>(val data: T) : UiModel<T>()
        class Error<T>(val message: Int = R.string.stats_loading_error) : UiModel<T>()
    }

    data class SnackbarMessage(@StringRes val message: Int): Event()
}
