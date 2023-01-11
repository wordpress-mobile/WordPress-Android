package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType
import org.wordpress.android.fluxc.store.StatsStore.StatsType
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
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
    fun mapInsights(
        useCaseModels: List<UseCaseModel>,
        showError: (Int) -> Unit
    ): UiModel {
        val insightUseCaseModels = useCaseModels.filter { it.type is InsightType }
        if (insightUseCaseModels.isNotEmpty()) {
            val allFailing = insightUseCaseModels.fold(true) { acc, useCaseModel ->
                acc && useCaseModel.state == ERROR
            }
            val allFailingWithoutData = insightUseCaseModels.fold(true) { acc, useCaseModel ->
                acc && useCaseModel.state == ERROR && useCaseModel.data == null
            }
            return if (!allFailing && !allFailingWithoutData) {
                val data = useCaseModels.map { useCaseModel ->
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
                }
                UiModel.Success(data)
            } else if (!allFailingWithoutData) {
                showError(getErrorMessage())
                UiModel.Success(useCaseModels.map { useCaseModel ->
                    StatsBlock.Error(
                        useCaseModel.type,
                        useCaseModel.data ?: useCaseModel.stateData ?: listOf()
                    )
                })
            } else {
                UiModel.Error(getErrorMessage())
            }
        } else {
            return UiModel.Empty(
                R.string.stats_empty_insights_title,
                R.string.stats_insights_management_title,
                R.drawable.img_illustration_insights_94dp,
                true
            )
        }
    }

    fun mapTimeStats(
        useCaseModels: List<UseCaseModel>,
        showError: (Int) -> Unit
    ): UiModel {
        return mapStatsWithOverview(TimeStatsType.OVERVIEW, useCaseModels, showError)
    }

    fun mapDetailStats(
        useCaseModels: List<UseCaseModel>,
        showError: (Int) -> Unit
    ): UiModel {
        return mapStatsWithOverview(PostDetailType.POST_OVERVIEW, useCaseModels, showError)
    }

    fun mapViewsVisitorsDetailStats(
        useCaseModels: List<UseCaseModel>,
        showError: (Int) -> Unit
    ): UiModel {
        return mapStatsWithOverview(TimeStatsType.OVERVIEW, useCaseModels, showError)
    }

    private fun mapStatsWithOverview(
        overViewType: StatsType,
        useCaseModels: List<UseCaseModel>,
        showError: (Int) -> Unit
    ): UiModel {
        val allFailing = useCaseModels.isNotEmpty() && useCaseModels
            .fold(true) { acc, useCaseModel ->
                acc && useCaseModel.state == ERROR
            }
        val overviewIsFailing = useCaseModels.any { it.type == overViewType && it.state == ERROR }
        val overviewHasData = useCaseModels.any { it.type == overViewType && it.data != null }
        return if (!allFailing && (overviewHasData || !overviewIsFailing)) {
            if (useCaseModels.isNotEmpty()) {
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
            } else {
                UiModel.Empty(R.string.loading)
            }
        } else if (overviewHasData) {
            showError(getErrorMessage())
            UiModel.Success(useCaseModels.mapNotNull { useCaseModel ->
                if ((useCaseModel.type == overViewType) && useCaseModel.data != null) {
                    StatsBlock.Success(useCaseModel.type, useCaseModel.data)
                } else {
                    useCaseModel.stateData?.let {
                        StatsBlock.Error(
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
            R.string.stats_loading_error
        } else {
            R.string.no_network_title
        }
    }
}
