package org.wordpress.android.ui.mysite

import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.QuickStartRepository.QuickStartModel.QuickStartCategory
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.util.mergeAsyncNotNull
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class QuickStartRepository
@Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val quickStartStore: QuickStartStore,
    private val quickStartUtils: QuickStartUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
    private val eventBus: EventBusWrapper
) : CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private val detailsMap: Map<QuickStartTask, QuickStartTaskDetails> = QuickStartTaskDetails.values()
            .associateBy { it.task }
    private val refresh = MutableLiveData<Boolean>()
    private val activeTask = MutableLiveData<QuickStartTask>()
    private val quickStartCategories: LiveData<List<QuickStartCategory>> = mergeAsyncNotNull(
            this,
            refresh,
            selectedSiteRepository.selectedSiteChange,
            distinct = false
    ) { _, site ->
        if (quickStartUtils.isQuickStartInProgress(site)) {
            val customizeCategory = buildQuickStartCategory(site, CUSTOMIZE)
            val growCategory = buildQuickStartCategory(site, GROW)
            listOfNotNull(customizeCategory, growCategory)
        } else {
            listOf()
        }
    }
    val quickStartModel: LiveData<QuickStartModel> = merge(quickStartCategories, activeTask) { categories, activeTask ->
        categories?.let {
            QuickStartModel(activeTask, categories)
        }
    }
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbar = _onSnackbar as LiveData<Event<SnackbarMessageHolder>>

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
        refresh.postValue(false)
    }

    fun setActiveTask(task: QuickStartTask) {
        activeTask.postValue(task)
        val shortQuickStartMessage =
                if (task == UPDATE_SITE_TITLE) {
                    HtmlCompat.fromHtml(
                            resourceProvider.getString(
                                    R.string.quick_start_dialog_update_site_title_message_short,
                                    SiteUtils.getSiteNameOrHomeURL(selectedSiteRepository.getSelectedSite())
                            ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                } else {
                    val activeTutorialPrompt = QuickStartMySitePrompts.getPromptDetailsForTask(task)
                    quickStartUtils.stylizeQuickStartPrompt(
                            activeTutorialPrompt!!.shortMessagePrompt,
                            activeTutorialPrompt.iconId
                    )
                }
        _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(shortQuickStartMessage))))
    }

    fun completeTask(task: QuickStartTask) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            // TODO Remove this before the feature is done
            // Uncomment this code to mark a task as not completed for testing purposes
//            if (quickStartStore.hasDoneTask(site.id.toLong(), task)) {
//                quickStartStore.setDoneTask(site.id.toLong(), task, false)
//                refresh.value = false
//                return
//            }
            if (task != activeTask.value) return
            activeTask.value = null
            if (quickStartStore.hasDoneTask(site.id.toLong(), task)) return
            // If we want notice and reminders, we should call QuickStartUtils.completeTaskAndRemindNextOne here
            quickStartStore.setDoneTask(site.id.toLong(), task, true)
            analyticsTrackerWrapper.track(quickStartUtils.getTaskCompletedTracker(task))
            refresh.value = false
            if (quickStartUtils.isEveryQuickStartTaskDone(site.id)) {
                analyticsTrackerWrapper.track(Stat.QUICK_START_ALL_TASKS_COMPLETED)
                val payload = CompleteQuickStartPayload(site, NEXT_STEPS.toString())
                dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
            }
        }
    }

    fun requestNextStepOfTask(task: QuickStartTask) {
        if (task != activeTask.value) return
        activeTask.value = null
        eventBus.postSticky(QuickStartEvent(task))
    }

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
