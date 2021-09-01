package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar.Callback.DISMISS_EVENT_SWIPE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
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
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.quickstart.QuickStartMySitePrompts
import org.wordpress.android.ui.quickstart.QuickStartNoticeDetails
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.mapAsync
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Suppress("LongParameterList", "TooManyFunctions")
@Singleton
class QuickStartRepository
@Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val quickStartStore: QuickStartStore,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dispatcher: Dispatcher,
    private val eventBus: EventBusWrapper,
    private val dynamicCardStore: DynamicCardStore,
    private val htmlCompat: HtmlCompatWrapper,
    private val mySiteImprovementsFeatureConfig: MySiteImprovementsFeatureConfig,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val contextProvider: ContextProvider,
    private val htmlMessageUtils: HtmlMessageUtils
) : CoroutineScope, MySiteSource<QuickStartUpdate> {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private val detailsMap: Map<QuickStartTask, QuickStartTaskDetails> = QuickStartTaskDetails.values()
            .associateBy { it.task }
    private val refresh = MutableLiveData<Boolean>()
    private val _activeTask = MutableLiveData<QuickStartTask?>()
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onQuickStartMySitePrompts = MutableLiveData<Event<QuickStartMySitePrompts>>()
    val onSnackbar = _onSnackbar as LiveData<Event<SnackbarMessageHolder>>
    val onQuickStartMySitePrompts = _onQuickStartMySitePrompts as LiveData<Event<QuickStartMySitePrompts>>
    val activeTask = _activeTask as LiveData<QuickStartTask?>
    var isQuickStartNoticeShown: Boolean = false

    private var pendingTask: QuickStartTask? = null

    private fun buildQuickStartCategory(siteLocalId: Int, quickStartTaskType: QuickStartTaskType) = QuickStartCategory(
            quickStartTaskType,
            uncompletedTasks = quickStartStore.getUncompletedTasksByType(siteLocalId.toLong(), quickStartTaskType)
                    .mapNotNull { detailsMap[it] },
            completedTasks = quickStartStore.getCompletedTasksByType(siteLocalId.toLong(), quickStartTaskType)
                    .mapNotNull { detailsMap[it] })

    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<QuickStartUpdate> {
        _activeTask.value = null
        pendingTask = null
        if (selectedSiteRepository.getSelectedSite()?.showOnFront == ShowOnFront.POSTS.value &&
                !quickStartStore.hasDoneTask(siteLocalId.toLong(), EDIT_HOMEPAGE)) {
            setTaskDoneAndTrack(EDIT_HOMEPAGE, siteLocalId)
            refresh()
        }
        val quickStartTaskTypes = refresh.mapAsync(coroutineScope) {
            getQuickStartTaskTypes(siteLocalId).onEach { taskType ->
                if (quickStartUtilsWrapper.isEveryQuickStartTaskDoneForType(siteLocalId, taskType)) {
                    onCategoryCompleted(siteLocalId, taskType)
                }
            }
        }
        return merge(quickStartTaskTypes, activeTask) { types, activeTask ->
            val categories = if (quickStartUtilsWrapper.isQuickStartInProgress(siteLocalId)) {
                types?.map { buildQuickStartCategory(siteLocalId, it) } ?: listOf()
            } else {
                listOf()
            }
            QuickStartUpdate(activeTask, categories)
        }
    }

    private suspend fun getQuickStartTaskTypes(siteLocalId: Int): List<QuickStartTaskType> {
        return if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
            dynamicCardStore.getCards(siteLocalId).dynamicCardTypes.map { it.toQuickStartTaskType() }
        } else {
            listOf(CUSTOMIZE, GROW)
        }
    }

    fun startQuickStart(siteLocalId: Int) {
        if (siteLocalId != SelectedSiteRepository.UNAVAILABLE) {
            quickStartUtilsWrapper.startQuickStart(siteLocalId)
            refresh()
        }
    }

    fun skipQuickStart() {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            val selectedSiteLocalId = selectedSite.id.toLong()
            QuickStartTask.values().forEach { quickStartStore.setDoneTask(selectedSiteLocalId, it, true) }
            quickStartStore.setQuickStartCompleted(selectedSiteLocalId, true)
            // skipping all tasks means no achievement notification, so we mark it as received
            quickStartStore.setQuickStartNotificationReceived(selectedSiteLocalId, true)
        }
    }

    fun refresh() {
        refresh.postValue(true)
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

    @JvmOverloads fun completeTask(task: QuickStartTask, refreshImmediately: Boolean = false) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            if (task != activeTask.value && task != pendingTask) return
            _activeTask.value = null
            pendingTask = null
            if (quickStartStore.hasDoneTask(selectedSite.id.toLong(), task)) return
            quickStartUtilsWrapper.completeTaskAndRemindNextOne(
                    task,
                    selectedSite,
                    QuickStartEvent(task),
                    contextProvider.getContext()
            )
            setTaskDoneAndTrack(task, selectedSite.id)
            // We need to refresh immediately. This is useful for tasks that are completed on the My Site screen.
            if (refreshImmediately) {
                refresh()
            }
            if (quickStartUtilsWrapper.isEveryQuickStartTaskDone(selectedSite.id)) {
                quickStartStore.setQuickStartCompleted(selectedSite.id.toLong(), true)
                analyticsTrackerWrapper.track(Stat.QUICK_START_ALL_TASKS_COMPLETED, mySiteImprovementsFeatureConfig)
                val payload = CompleteQuickStartPayload(selectedSite, NEXT_STEPS.toString())
                dispatcher.dispatch(SiteActionBuilder.newCompleteQuickStartAction(payload))
            }
        }
    }

    private fun setTaskDoneAndTrack(
        task: QuickStartTask,
        siteLocalId: Int
    ) {
        quickStartStore.setDoneTask(siteLocalId.toLong(), task, true)
        analyticsTrackerWrapper.track(
                quickStartUtilsWrapper.getTaskCompletedTracker(task),
                mySiteImprovementsFeatureConfig
        )
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

    private suspend fun onCategoryCompleted(siteLocalId: Int, categoryType: QuickStartTaskType) {
        if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
            val completionMessage = getCategoryCompletionMessage(categoryType)
            _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(completionMessage.asHtml()))))
            dynamicCardStore.removeCard(siteLocalId, categoryType.toDynamicCardType())
        }
    }

    private fun getCategoryCompletionMessage(taskType: QuickStartTaskType) = when (taskType) {
        CUSTOMIZE -> R.string.quick_start_completed_type_customize_message
        GROW -> R.string.quick_start_completed_type_grow_message
        UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
    }.let { resourceProvider.getString(it) }

    private fun String.asHtml() = htmlCompat.fromHtml(this)

    private fun DynamicCardType.toQuickStartTaskType(): QuickStartTaskType {
        return when (this) {
            CUSTOMIZE_QUICK_START -> CUSTOMIZE
            GROW_QUICK_START -> GROW
        }
    }

    private fun QuickStartTaskType.toDynamicCardType(): DynamicCardType {
        return when (this) {
            CUSTOMIZE -> CUSTOMIZE_QUICK_START
            GROW -> GROW_QUICK_START
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    fun checkAndShowQuickStartNotice() {
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSite()?.id ?: -1
        if (quickStartUtilsWrapper.isQuickStartInProgress(selectedSiteLocalId) &&
                appPrefsWrapper.isQuickStartNoticeRequired()) {
            showQuickStartNotice(selectedSiteLocalId)
        }
    }

    private fun showQuickStartNotice(selectedSiteLocalId: Int) {
        val taskToPrompt = quickStartUtilsWrapper.getNextUncompletedQuickStartTask(selectedSiteLocalId.toLong())
        if (taskToPrompt != null) {
            analyticsTrackerWrapper.track(Stat.QUICK_START_TASK_DIALOG_VIEWED)
            appPrefsWrapper.setQuickStartNoticeRequired(false)
            val taskNoticeDetails = QuickStartNoticeDetails.getNoticeForTask(taskToPrompt)
            val message = htmlMessageUtils.getHtmlMessageFromStringFormat(
                    "<b>${resourceProvider.getString(taskNoticeDetails.titleResId)}</b>:" +
                            " ${resourceProvider.getString(taskNoticeDetails.messageResId)}"
            )
            isQuickStartNoticeShown = true
            _onSnackbar.value = Event(
                    SnackbarMessageHolder(
                            message = UiStringText(message),
                            buttonTitle = UiStringRes(R.string.quick_start_button_positive),
                            buttonAction = { onQuickStartNoticeButtonAction(taskToPrompt) },
                            onDismissAction = { event ->
                                isQuickStartNoticeShown = false
                                if (event == DISMISS_EVENT_SWIPE) onQuickStartNoticeNegativeAction(taskToPrompt)
                            },
                            duration = QUICK_START_NOTICE_DURATION
                    )
            )
        }
    }

    private fun onQuickStartNoticeButtonAction(task: QuickStartTask) {
        analyticsTrackerWrapper.track(Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED)
        setActiveTask(task)
    }

    private fun onQuickStartNoticeNegativeAction(task: QuickStartTask) {
        analyticsTrackerWrapper.track(Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED)
        appPrefsWrapper.setLastSkippedQuickStartTask(task)
    }

    data class QuickStartCategory(
        val taskType: QuickStartTaskType,
        val uncompletedTasks: List<QuickStartTaskDetails>,
        val completedTasks: List<QuickStartTaskDetails>
    )

    companion object {
        private const val QUICK_START_NOTICE_DURATION = 7000
    }
}
