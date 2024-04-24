package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar.Callback.DISMISS_EVENT_SWIPE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartNoticeDetails
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.quickstart.QuickStartType.ExistingSiteQuickStartType
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ContextProvider
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
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val dispatcher: Dispatcher,
    private val eventBus: EventBusWrapper,
    private val htmlCompat: HtmlCompatWrapper,
    private val contextProvider: ContextProvider,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val quickStartTracker: QuickStartTracker,
) : CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private val detailsMap: Map<String, QuickStartTaskDetails> = QuickStartTaskDetails.values()
        .associateBy { it.taskString }
    private val _activeTask = MutableLiveData<QuickStartTask?>()
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onQuickStartMySitePrompts = MutableLiveData<Event<QuickStartMySitePrompts>>()
    private val _quickStartMenuStep = MutableLiveData<QuickStartMenuStep?>()
    private var _isQuickStartNoticeShown: Boolean = false
    val onSnackbar = _onSnackbar as LiveData<Event<SnackbarMessageHolder>>
    val onQuickStartMySitePrompts = _onQuickStartMySitePrompts as LiveData<Event<QuickStartMySitePrompts>>
    val activeTask = _activeTask as LiveData<QuickStartTask?>
    val quickStartMenuStep = _quickStartMenuStep as LiveData<QuickStartMenuStep?>
    val isQuickStartNoticeShown = _isQuickStartNoticeShown
    val quickStartType: QuickStartType
        get() = selectedSiteRepository.getSelectedSite()?.let {
            val siteLocalId = it.id.toLong()
            appPrefsWrapper.getLastSelectedQuickStartTypeForSite(siteLocalId)
        } ?: NewSiteQuickStartType
    private var pendingTask: QuickStartTask? = null

    fun buildQuickStartCategory(siteLocalId: Int, quickStartTaskType: QuickStartTaskType) = QuickStartCategory(
        quickStartTaskType,
        uncompletedTasks = quickStartStore.getUncompletedTasksByType(siteLocalId.toLong(), quickStartTaskType)
            .mapNotNull { detailsMap[it.string] },
        completedTasks = quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), quickStartTaskType)
            .mapNotNull { detailsMap[it.string] })

    fun resetTask() {
        clearActiveTask()
        clearPendingTask()
        clearMenuStep()
    }

    fun clearActiveTask() {
        _activeTask.postValue(null)
    }

    fun clearPendingTask() {
        pendingTask = null
    }

    fun checkAndSetQuickStartType(isNewSite: Boolean) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            val siteLocalId = selectedSite.id.toLong()
            val quickStartType = if (isNewSite) NewSiteQuickStartType else ExistingSiteQuickStartType
            appPrefsWrapper.setLastSelectedQuickStartTypeForSite(quickStartType, siteLocalId)
        }
    }

    fun getQuickStartTaskTypes(): List<QuickStartTaskType> {
        return quickStartType.taskTypes.filterNot { it == UNKNOWN }
    }

    fun skipQuickStart() {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            val selectedSiteLocalId = selectedSite.id.toLong()
            QuickStartTask.getAllTasks().forEach { quickStartStore.setDoneTask(selectedSiteLocalId, it, true) }
            quickStartStore.setQuickStartCompleted(selectedSiteLocalId, true)
            // skipping all tasks means no achievement notification, so we mark it as received
            quickStartStore.setQuickStartNotificationReceived(selectedSiteLocalId, true)
        }
    }

    fun setActiveTask(task: QuickStartTask, isFromMenu: Boolean = false) {
        _activeTask.postValue(task)
        clearPendingTask()
        clearMenuStep()
        when {
            !isFromMenu && task.isShownInMenu() -> requestMoreStepForTask(task)
            task == QuickStartNewSiteTask.UPDATE_SITE_TITLE -> {
                val shortQuickStartMessage = resourceProvider.getString(
                    R.string.quick_start_dialog_update_site_title_message_short,
                    SiteUtils.getSiteNameOrHomeURL(selectedSiteRepository.getSelectedSite())
                )
                _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(shortQuickStartMessage.asHtml()))))
            }
            task == quickStartType.getTaskFromString(QuickStartStore.QUICK_START_VIEW_SITE_LABEL) -> {
                val shortQuickStartMessage = resourceProvider.getString(
                    R.string.quick_start_dialog_view_your_site_message_short,
                    SiteUtils.getHomeURLOrHostName(selectedSiteRepository.getSelectedSite())
                )
                _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(shortQuickStartMessage.asHtml()))))
            }
            else -> {
                QuickStartMySitePrompts.getPromptDetailsForTask(task)?.let { activeTutorialPrompt ->
                    _onQuickStartMySitePrompts.postValue(Event(activeTutorialPrompt))
                }
            }
        }
    }

    fun isPendingTask(task: QuickStartTask) = task == pendingTask

    fun completeTask(task: QuickStartTask) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            if (task != activeTask.value && task != pendingTask) return
            resetTask()
            if (quickStartStore.hasDoneTask(selectedSite.id.toLong(), task)) return
            quickStartUtilsWrapper.completeTaskAndRemindNextOne(
                task,
                selectedSite,
                QuickStartEvent(task),
                contextProvider.getContext(),
                quickStartType
            )
            setTaskDoneAndTrack(task, selectedSite.id)
            if (quickStartType.isEveryQuickStartTaskDone(quickStartStore, selectedSite.id.toLong())) {
                quickStartStore.setQuickStartCompleted(selectedSite.id.toLong(), true)
                quickStartTracker.track(Stat.QUICK_START_ALL_TASKS_COMPLETED)
                val payload = CompleteQuickStartPayload(selectedSite, NEXT_STEPS.toString())
                dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
                showCompletedQuickStartNotice()
            }
        }
    }

    private fun setTaskDoneAndTrack(
        task: QuickStartTask,
        siteLocalId: Int
    ) {
        quickStartStore.setDoneTask(siteLocalId.toLong(), task, true)
        quickStartTracker.track(quickStartUtilsWrapper.getTaskCompletedTracker(task))
    }

    private fun requestMoreStepForTask(task: QuickStartTask) {
        clearActiveTask()
        pendingTask = task
        val shortQuickStartMessage = resourceProvider.getString(
            R.string.quick_start_site_menu_tab_message_short,
            resourceProvider.getString(R.string.more)
        )

        _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(htmlCompat.fromHtml(shortQuickStartMessage)))))
        _quickStartMenuStep.postValue(QuickStartMenuStep(true, task))
    }

    fun requestNextStepOfTask(task: QuickStartTask) {
        if (task != activeTask.value) return
        clearActiveTask()
        pendingTask = task
        eventBus.postSticky(QuickStartEvent(task))
    }

    fun clear() {
        job.cancel()
    }

    private fun String.asHtml() = htmlCompat.fromHtml(this)

    fun checkAndShowQuickStartNotice() {
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSite()?.id ?: -1
        if (quickStartType.isQuickStartInProgress(quickStartStore, selectedSiteLocalId.toLong()) &&
            appPrefsWrapper.isQuickStartNoticeRequired()
        ) {
            showQuickStartNotice(selectedSiteLocalId)
        }
    }

    private fun showCompletedQuickStartNotice() {
        launch {
            delay(QUICK_START_COMPLETED_NOTICE_DELAY)
            val message = htmlMessageUtils.getHtmlMessageFromStringFormat(
                "<b>${resourceProvider.getString(R.string.quick_start_completed_tour_title)}</b>" +
                        " ${resourceProvider.getString(R.string.quick_start_completed_tour_subtitle)}"
            )
            _onSnackbar.postValue(
                Event(
                    SnackbarMessageHolder(
                        message = UiStringText(message),
                        duration = QUICK_START_NOTICE_DURATION,
                        isImportant = false
                    )
                )
            )
        }
    }

    private fun showQuickStartNotice(selectedSiteLocalId: Int) {
        val taskToPrompt = quickStartUtilsWrapper
            .getNextUncompletedQuickStartTask(quickStartType, selectedSiteLocalId.toLong())
        if (taskToPrompt != null) {
            quickStartTracker.track(Stat.QUICK_START_TASK_DIALOG_VIEWED)
            appPrefsWrapper.setQuickStartNoticeRequired(false)
            val taskNoticeDetails = QuickStartNoticeDetails.getNoticeForTask(taskToPrompt) ?: return
            val message = htmlMessageUtils.getHtmlMessageFromStringFormat(
                "<b>${resourceProvider.getString(taskNoticeDetails.titleResId)}</b>:" +
                        " ${resourceProvider.getString(taskNoticeDetails.messageResId)}"
            )
            _isQuickStartNoticeShown = true
            _onSnackbar.value = Event(
                SnackbarMessageHolder(
                    message = UiStringText(message),
                    buttonTitle = UiStringRes(R.string.quick_start_button_positive),
                    buttonAction = { onQuickStartNoticeButtonAction(taskToPrompt) },
                    onDismissAction = { event ->
                        _isQuickStartNoticeShown = false
                        if (event == DISMISS_EVENT_SWIPE) onQuickStartNoticeNegativeAction(taskToPrompt)
                    },
                    duration = QUICK_START_NOTICE_DURATION,
                    isImportant = false
                )
            )
        }
    }

    fun shouldShowNextStepsCard(siteId: Long) = appPrefsWrapper.getShouldHideNextStepsDashboardCard(siteId).not()

    fun onHideNextStepsCard(siteId: Long) = appPrefsWrapper.setShouldHideNextStepsDashboardCard(siteId, true)

    fun shouldShowGetToKnowTheAppCard(siteId: Long) =
        appPrefsWrapper.getShouldHideGetToKnowTheAppDashboardCard(siteId).not()

    fun onHideShowGetToKnowTheAppCard(siteId: Long) =
        appPrefsWrapper.setShouldHideGetToKnowTheAppDashboardCard(siteId, true)

    private fun onQuickStartNoticeButtonAction(task: QuickStartTask) {
        quickStartTracker.track(Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED)
        setActiveTask(task)
    }

    private fun onQuickStartNoticeNegativeAction(task: QuickStartTask) {
        quickStartTracker.track(Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED)
        appPrefsWrapper.setLastSkippedQuickStartTask(task)
    }

    private fun QuickStartTask.isShownInMenu() =
        when (this) {
            quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL),
            quickStartType.getTaskFromString(QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL),
            QuickStartNewSiteTask.REVIEW_PAGES,
            QuickStartNewSiteTask.CHECK_STATS,
            QuickStartNewSiteTask.ENABLE_POST_SHARING -> true
            else -> false
        }

    fun clearMenuStep() {
        if (_quickStartMenuStep.value != null) {
            _quickStartMenuStep.postValue(null)
        }
    }

    data class QuickStartCategory(
        val taskType: QuickStartTaskType,
        val uncompletedTasks: List<QuickStartTaskDetails>,
        val completedTasks: List<QuickStartTaskDetails>
    )

    data class QuickStartMenuStep(
        val isStarted: Boolean,
        val task: QuickStartTask? = null
    )
    companion object {
        private const val QUICK_START_NOTICE_DURATION = 7000
        private const val QUICK_START_COMPLETED_NOTICE_DELAY = 5000L
    }
}
