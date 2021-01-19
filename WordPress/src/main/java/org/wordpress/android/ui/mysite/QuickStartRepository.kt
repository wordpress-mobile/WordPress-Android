package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.mergeAsyncNotNull
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class QuickStartRepository
@Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val quickStartStore: QuickStartStore,
    private val quickStartUtils: QuickStartUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) : CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private val detailsMap: Map<QuickStartTask, QuickStartTaskDetails> = QuickStartTaskDetails.values()
            .associateBy { it.task }
    private val refresh = MutableLiveData<Boolean>()
    private val activeTask = MutableLiveData<QuickStartTask>()
    val quickStartModel: LiveData<QuickStartModel> = mergeAsyncNotNull(
            this,
            refresh,
            activeTask,
            selectedSiteRepository.selectedSiteChange
    ) { _, activeTask, site ->
        val customizeCategory = buildQuickStartCategory(site, CUSTOMIZE)
        val growCategory = buildQuickStartCategory(site, GROW)
        QuickStartModel(activeTask, listOfNotNull(customizeCategory, growCategory))
    }

    private fun buildQuickStartCategory(site: SiteModel, quickStartTaskType: QuickStartTaskType) = QuickStartCategory(
            quickStartTaskType,
            uncompletedTasks = quickStartStore.getUncompletedTasksByType(site.id.toLong(), quickStartTaskType)
                    .mapNotNull { detailsMap[it] },
            completedTasks = quickStartStore.getCompletedTasksByType(site.id.toLong(), quickStartTaskType)
                    .mapNotNull { detailsMap[it] })

    fun startQuickStart() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            quickStartStore.setDoneTask(site.id.toLong(), CREATE_SITE, true)
            refresh.postValue(false)
        }
    }

    fun refreshIfNecessary() {
        if (quickStartModel.value == null && quickStartUtils.isQuickStartInProgress()) {
            refresh.postValue(false)
        }
    }

    data class QuickStartCategory(
        val taskType: QuickStartTaskType,
        val uncompletedTasks: List<QuickStartTaskDetails>,
        val completedTasks: List<QuickStartTaskDetails>
    )

    fun clear() {
        job.cancel()
    }

    data class QuickStartModel(
        val activeTask: QuickStartTask? = null,
        val categories: List<QuickStartCategory> = listOf()
    ) {
        data class QuickStartCategory(
            val taskType: QuickStartTaskType,
            val uncompletedTasks: List<QuickStartTaskDetails>,
            val completedTasks: List<QuickStartTaskDetails>
        )
    }
}
