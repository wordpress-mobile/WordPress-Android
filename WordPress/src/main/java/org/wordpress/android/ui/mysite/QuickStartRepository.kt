package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.util.QuickStartUtils
import org.wordpress.android.util.merge
import javax.inject.Inject

class QuickStartRepository
@Inject constructor(
    private val quickStartStore: QuickStartStore,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private val detailsMap: Map<QuickStartTask, QuickStartTaskDetails> = QuickStartTaskDetails.values()
            .associateBy { it.task }
    private val customizeTasks = MutableLiveData<QuickStartCategory>()
    private val growTasks = MutableLiveData<QuickStartCategory>()
    val quickStartModel: LiveData<List<QuickStartCategory>> = merge(customizeTasks, growTasks)
    { customizeCategory, growCategory ->
        listOfNotNull(customizeCategory, growCategory)
    }

    fun startQuickStart() {
        quickStartStore.setDoneTask(AppPrefs.getSelectedSite().toLong(), CREATE_SITE, true)
        refreshCustomizeTasks()
        refreshGrowTasks()
    }

    fun refreshIfNecessary() {
        if (quickStartModel.value == null && QuickStartUtils.isQuickStartInProgress(quickStartStore)) {
            refreshCustomizeTasks()
            refreshGrowTasks()
        }
    }

    private fun refreshCustomizeTasks() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            customizeTasks.postValue(
                    QuickStartCategory(
                            CUSTOMIZE,
                            uncompletedTasks = quickStartStore.getUncompletedTasksByType(site.siteId, CUSTOMIZE)
                                    .mapNotNull { detailsMap[it] },
                            completedTasks = quickStartStore.getCompletedTasksByType(site.siteId, CUSTOMIZE)
                                    .mapNotNull { detailsMap[it] })
            )
        }
    }

    private fun refreshGrowTasks() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            growTasks.postValue(
                    QuickStartCategory(
                            GROW,
                            uncompletedTasks = quickStartStore.getUncompletedTasksByType(site.siteId, GROW)
                                    .mapNotNull { detailsMap[it] },
                            completedTasks = quickStartStore.getCompletedTasksByType(site.siteId, GROW)
                                    .mapNotNull { detailsMap[it] })
            )
        }
    }

    data class QuickStartCategory(
        val taskType: QuickStartTaskType,
        val uncompletedTasks: List<QuickStartTaskDetails>,
        val completedTasks: List<QuickStartTaskDetails>
    )
}

