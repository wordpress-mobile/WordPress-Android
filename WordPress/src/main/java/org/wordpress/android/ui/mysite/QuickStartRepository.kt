package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.CREATE_SITE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.EDIT_HOMEPAGE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class QuickStartRepository
@Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val quickStartStore: QuickStartStore,
    private val quickStartUtils: QuickStartUtilsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
    private val eventBus: EventBusWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val htmlCompat: HtmlCompatWrapper
) : CoroutineScope, MySiteSource<QuickStartUpdate> {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private val detailsMap: Map<QuickStartTask, QuickStartTaskDetails> = QuickStartTaskDetails.values()
            .associateBy { it.task }
    private val quickStartTaskTypes = MutableLiveData<Set<QuickStartTaskType>>()
    private val _activeTask = MutableLiveData<QuickStartTask?>()
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onQuickStartMySitePrompts = MutableLiveData<Event<QuickStartMySitePrompts>>()
    val onSnackbar = _onSnackbar as LiveData<Event<SnackbarMessageHolder>>
    val onQuickStartMySitePrompts = _onQuickStartMySitePrompts as LiveData<Event<QuickStartMySitePrompts>>
    val activeTask = _activeTask as LiveData<QuickStartTask?>

    private var pendingTask: QuickStartTask? = null
    private var pendingCategoryCompletion: QuickStartTaskType? = null

    init {
        quickStartTaskTypes.value = setOf(CUSTOMIZE, GROW)
                .filter { !appPrefsWrapper.isQuickStartTaskTypeRemoved(it) }
                .toSet()
    }

    private fun buildQuickStartCategory(siteId: Int, quickStartTaskType: QuickStartTaskType) = QuickStartCategory(
            quickStartTaskType,
            uncompletedTasks = quickStartStore.getUncompletedTasksByType(siteId.toLong(), quickStartTaskType)
                    .mapNotNull { detailsMap[it] },
            completedTasks = quickStartStore.getCompletedTasksByType(siteId.toLong(), quickStartTaskType)
                    .mapNotNull { detailsMap[it] })

    override fun buildSource(coroutineScope: CoroutineScope, siteId: Int): LiveData<QuickStartUpdate> {
        _activeTask.value = null
        pendingTask = null
        if (selectedSiteRepository.getSelectedSite()?.showOnFront == ShowOnFront.POSTS.value &&
                !quickStartStore.hasDoneTask(siteId.toLong(), EDIT_HOMEPAGE)) {
            quickStartStore.setDoneTask(siteId.toLong(), EDIT_HOMEPAGE, true)
            refresh()
        }
        return merge(quickStartTaskTypes, activeTask) { types, activeTask ->
            val categories = if (quickStartUtils.isQuickStartInProgress(siteId)) {
                types?.map { buildQuickStartCategory(siteId, it) } ?: listOf()
            } else {
                listOf()
            }
            QuickStartUpdate(activeTask, categories)
        }
    }

    fun startQuickStart() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            quickStartStore.setDoneTask(site.id.toLong(), CREATE_SITE, true)
            refresh()
        }
    }

    fun refresh() {
        quickStartTaskTypes.postValue(quickStartTaskTypes.value)
        showCategoryCompletionMessageIfNeeded()
    }

    fun setActiveTask(task: QuickStartTask) {
        _activeTask.postValue(task)
        pendingTask = null
        if (task == UPDATE_SITE_TITLE) {
            val shortQuickStartMessage = resourceProvider.getString(
                    R.string.quick_start_dialog_update_site_title_message_short,
                    SiteUtils.getSiteNameOrHomeURL(selectedSiteRepository.getSelectedSite())
            )
            _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(shortQuickStartMessage.asHtml()))))
        } else {
            QuickStartMySitePrompts.getPromptDetailsForTask(task)?.let { activeTutorialPrompt ->
                _onQuickStartMySitePrompts.postValue(Event(activeTutorialPrompt))
            }
        }
    }

    fun clearActiveTask() {
        _activeTask.value = null
    }

    fun completeTask(task: QuickStartTask, refreshImmediately: Boolean = false) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            // TODO Remove this before the feature is done
            // Uncomment this code to mark a task as not completed for testing purposes
//            if (quickStartStore.hasDoneTask(site.id.toLong(), task)) {
//                quickStartStore.setDoneTask(site.id.toLong(), task, false)
//                refresh.value = false
//                return
//            }
            if (task != activeTask.value && task != pendingTask) return
            _activeTask.value = null
            pendingTask = null
            if (quickStartStore.hasDoneTask(site.id.toLong(), task)) return
            // If we want notice and reminders, we should call QuickStartUtils.completeTaskAndRemindNextOne here
            quickStartStore.setDoneTask(site.id.toLong(), task, true)
            analyticsTrackerWrapper.track(quickStartUtils.getTaskCompletedTracker(task))
            if (quickStartUtils.isEveryQuickStartTaskDoneForType(site.id, task.taskType)) {
                pendingCategoryCompletion = task.taskType
            }
            // We need to refresh immediately. This is useful for tasks that are completed on the My Site screen.
            if (refreshImmediately) {
                refresh()
            }
            if (quickStartUtils.isEveryQuickStartTaskDone(site.id)) {
                analyticsTrackerWrapper.track(Stat.QUICK_START_ALL_TASKS_COMPLETED)
                val payload = CompleteQuickStartPayload(site, NEXT_STEPS.toString())
                dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
            }
        }
    }

    fun requestNextStepOfTask(task: QuickStartTask) {
        if (task != activeTask.value) return
        _activeTask.value = null
        pendingTask = task
        eventBus.postSticky(QuickStartEvent(task))
    }

    fun clear() {
        job.cancel()
    }

    fun hideCategory(dynamicCardType: DynamicCardType) {
        val hiddenCategory = dynamicCardType.toQuickStartTaskType()
        hideQuickStartType(hiddenCategory)
    }

    fun removeCategory(dynamicCardType: DynamicCardType) {
        val removedQuickStartTaskType = dynamicCardType.toQuickStartTaskType()
        appPrefsWrapper.removeQuickStartTaskType(removedQuickStartTaskType)
        hideQuickStartType(removedQuickStartTaskType)
    }

    private fun DynamicCardType.toQuickStartTaskType(): QuickStartTaskType {
        return when (this) {
            CUSTOMIZE_QUICK_START -> CUSTOMIZE
            GROW_QUICK_START -> GROW
        }
    }

    private fun hideQuickStartType(hiddenCategory: QuickStartTaskType) {
        val currentCategories = (quickStartTaskTypes.value ?: setOf()).toMutableSet()
        currentCategories.remove(hiddenCategory)
        quickStartTaskTypes.value = currentCategories
    }

    private fun showCategoryCompletionMessageIfNeeded() = pendingCategoryCompletion?.let { taskType ->
        pendingCategoryCompletion = null
        val completionMessage = getCategoryCompletionMessage(taskType)
        _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(completionMessage.asHtml()))))
    }

    private fun getCategoryCompletionMessage(taskType: QuickStartTaskType) = when (taskType) {
        CUSTOMIZE -> R.string.quick_start_completed_type_customize_message
        GROW -> R.string.quick_start_completed_type_grow_message
        UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
    }.let { resourceProvider.getString(it) }

    private fun String.asHtml() = htmlCompat.fromHtml(this)

    data class QuickStartCategory(
        val taskType: QuickStartTaskType,
        val uncompletedTasks: List<QuickStartTaskDetails>,
        val completedTasks: List<QuickStartTaskDetails>
    )
}
