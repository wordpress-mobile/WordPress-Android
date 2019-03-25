package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes
import org.wordpress.android.fluxc.store.StatsStore.StatsTypes
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.LOADING
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class UiModelMapper
@Inject constructor(private val networkUtilsWrapper: NetworkUtilsWrapper) {
    fun mapInsights(useCaseModels: List<UseCaseModel>, showError: (Int) -> Unit): UiModel {
        val allFailing = useCaseModels.isNotEmpty() && useCaseModels.fold(true) { acc, useCaseModel ->
            acc && useCaseModel.state == ERROR
        }
        val allFailingWithoutData = useCaseModels.isNotEmpty() && useCaseModels.fold(true) { acc, useCaseModel ->
            acc && useCaseModel.state == ERROR && useCaseModel.data == null
        }
        return if (!allFailing && !allFailingWithoutData) {
            UiModel.Success(useCaseModels.map { useCaseModel ->
                when (useCaseModel.state) {
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
            showError(getErrorMessage())
            UiModel.Success(useCaseModels.map { useCaseModel ->
                Error(
                        useCaseModel.type,
                        useCaseModel.data ?: useCaseModel.stateData ?: listOf()
                )
            })
        } else {
            UiModel.Error(getErrorMessage())
        }
    }

    fun mapTimeStats(useCaseModels: List<UseCaseModel>, showError: (Int) -> Unit): UiModel {
        return mapStatsWithOverview(TimeStatsTypes.OVERVIEW, useCaseModels, showError)
    }

    fun mapDetailStats(useCaseModels: List<UseCaseModel>, showError: (Int) -> Unit): UiModel {
        return mapStatsWithOverview(PostDetailTypes.POST_OVERVIEW, useCaseModels, showError)
    }

    private fun mapStatsWithOverview(
        overViewType: StatsTypes,
        useCaseModels: List<UseCaseModel>,
        showError: (Int) -> Unit
    ): UiModel {
        val allFailing = useCaseModels.isNotEmpty() && useCaseModels
                .fold(true) { acc, useCaseModel ->
                    acc && useCaseModel.state == ERROR
                }
        val overviewHasData = useCaseModels.any { it.type == overViewType && it.data != null }
        return if (!allFailing) {
            UiModel.Success(useCaseModels.mapNotNull { useCaseModel ->
                if ((useCaseModel.type == overViewType) && useCaseModel.data != null) {
                    StatsBlock.Success(useCaseModel.type, useCaseModel.data)
                } else {
                    when (useCaseModel.state) {
                        SUCCESS -> StatsBlock.Success(useCaseModel.type, useCaseModel.data ?: listOf())
                        ERROR -> useCaseModel.stateData?.let {
                            StatsBlock.Error(
                                    useCaseModel.type,
                                    useCaseModel.stateData
                            )
                        }
                        LOADING -> useCaseModel.stateData?.let {
                            StatsBlock.Loading(
                                    useCaseModel.type,
                                    useCaseModel.stateData
                            )
                        }
                        EMPTY -> useCaseModel.stateData?.let {
                            StatsBlock.EmptyBlock(
                                    useCaseModel.type,
                                    useCaseModel.stateData
                            )
                        }
                    }
                }
            })
        } else if (overviewHasData) {
            showError(getErrorMessage())
            UiModel.Success(useCaseModels.mapNotNull { useCaseModel ->
                if ((useCaseModel.type == overViewType) && useCaseModel.data != null) {
                    StatsBlock.Success(useCaseModel.type, useCaseModel.data)
                } else {
                    useCaseModel.stateData?.let {
                        Error(
                                useCaseModel.type,
                                useCaseModel.stateData
                        )
                    }
                }
            })
        } else {
            UiModel.Error(getErrorMessage())
        }
    }

    private fun getErrorMessage(): Int {
        return if (networkUtilsWrapper.isNetworkAvailable()) {
            string.stats_loading_error
        } else {
            string.no_network_title
        }
    }
}
