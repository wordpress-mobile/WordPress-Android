package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.filter
import org.wordpress.android.util.mapAsync
import org.wordpress.android.util.merge
import javax.inject.Inject

class QuickStartCardSource @Inject constructor(
    private val quickStartRepository: QuickStartRepository,
    private val quickStartStore: QuickStartStore,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) : MySiteRefreshSource<QuickStartUpdate> {
    override val refresh = MutableLiveData(false)

    override fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<QuickStartUpdate> {
        quickStartRepository.resetTask()
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite?.showOnFront == ShowOnFront.POSTS.value) {
            refresh()
        }
        val quickStartTaskTypes = refresh.filter { it == true }.mapAsync(coroutineScope) {
            quickStartRepository.getQuickStartTaskTypes()
        }
        return merge(quickStartTaskTypes, quickStartRepository.activeTask) { types, activeTask ->
            val categories =
                if (selectedSite != null &&
                    quickStartUtilsWrapper.isQuickStartAvailableForTheSite(selectedSite) &&
                    quickStartRepository.quickStartType
                        .isQuickStartInProgress(quickStartStore, siteLocalId.toLong())
                ) {
                    types?.map { quickStartRepository.buildQuickStartCategory(siteLocalId, it) }
                        ?.filter { !isEmptyCategory(siteLocalId, it.taskType) } ?: listOf()
                } else {
                    listOf()
                }

            if (shouldShowQuickStartCard(categories, selectedSite)) {
                getState(QuickStartUpdate(activeTask, categories))
            } else {
                getState(QuickStartUpdate(null, listOf()))
            }
        }
    }

    private fun shouldShowQuickStartCard(
        categories: List<QuickStartRepository.QuickStartCategory>,
        selectedSite: SiteModel?
    ) : Boolean {
        selectedSite?.let { site ->
            return if (categories.any { it.taskType == QuickStartTaskType.GET_TO_KNOW_APP }) {
                quickStartRepository.shouldShowGetToKnowTheAppCard(site.siteId)
            } else {
                quickStartRepository.shouldShowNextStepsCard(site.siteId)
            }
        }
        return false
    }

    private fun isEmptyCategory(
        siteLocalId: Int,
        taskType: QuickStartTaskType
    ): Boolean {
        val completedTasks = quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), taskType)
        val unCompletedTasks = quickStartStore.getUncompletedTasksByType(siteLocalId.toLong(), taskType)
        return (completedTasks + unCompletedTasks).isEmpty()
    }
}
