package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
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
        if (selectedSiteRepository.getSelectedSite()?.showOnFront == ShowOnFront.POSTS.value &&
                !quickStartStore.hasDoneTask(siteLocalId.toLong(), EDIT_HOMEPAGE)) {
            quickStartRepository.setTaskDoneAndTrack(EDIT_HOMEPAGE, siteLocalId)
            refresh()
        }
        val quickStartTaskTypes = refresh.filter { it == true }.mapAsync(coroutineScope) {
            quickStartRepository.getQuickStartTaskTypes(siteLocalId).onEach { taskType ->
                if (!isEmptyCategory(siteLocalId, taskType) &&
                        quickStartUtilsWrapper.isEveryQuickStartTaskDoneForType(siteLocalId, taskType)) {
                    quickStartRepository.onCategoryCompleted(siteLocalId, taskType)
                }
            }
        }
        return merge(quickStartTaskTypes, quickStartRepository.activeTask) { types, activeTask ->
            val categories = if (quickStartUtilsWrapper.isQuickStartInProgress(siteLocalId)) {
                types?.map { quickStartRepository.buildQuickStartCategory(siteLocalId, it) }
                        ?.filter { !isEmptyCategory(siteLocalId, it.taskType) } ?: listOf()
            } else {
                listOf()
            }
            getState(QuickStartUpdate(activeTask, categories))
        }
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
