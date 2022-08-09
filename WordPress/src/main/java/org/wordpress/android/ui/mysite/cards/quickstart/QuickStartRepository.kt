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
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.DynamicCardType.CUSTOMIZE_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GET_TO_KNOW_APP_QUICK_START
import org.wordpress.android.fluxc.model.DynamicCardType.GROW_QUICK_START
import org.wordpress.android.fluxc.store.DynamicCardStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.CUSTOMIZE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GET_TO_KNOW_APP
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.GROW
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType.UNKNOWN
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartPayload
import org.wordpress.android.fluxc.store.SiteStore.CompleteQuickStartVariant.NEXT_STEPS
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
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
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.HtmlCompatWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.config.QuickStartExistingUsersV2FeatureConfig
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
    private val dynamicCardStore: DynamicCardStore,
    private val htmlCompat: HtmlCompatWrapper,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val contextProvider: ContextProvider,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val quickStartTracker: QuickStartTracker,
    buildConfigWrapper: BuildConfigWrapper,
    mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig,
    quickStartForExistingUsersV2FeatureConfig: QuickStartExistingUsersV2FeatureConfig
) : CoroutineScope {
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private val detailsMap: Map<String, QuickStartTaskDetails> = QuickStartTaskDetails.values()
            .associateBy { it.taskString }
    private val _activeTask = MutableLiveData<QuickStartTask?>()
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onQuickStartMySitePrompts = MutableLiveData<Event<QuickStartMySitePrompts>>()
    private val _onQuickStartTabStep = MutableLiveData<QuickStartTabStep?>()
    private var _isQuickStartNoticeShown: Boolean = false
    private val isMySiteTabsEnabled = mySiteDashboardTabsFeatureConfig.isEnabled() &&
            buildConfigWrapper.isMySiteTabsEnabled &&
            selectedSiteRepository.getSelectedSite()?.isUsingWpComRestApi ?: true
    val onSnackbar = _onSnackbar as LiveData<Event<SnackbarMessageHolder>>
    val onQuickStartMySitePrompts = _onQuickStartMySitePrompts as LiveData<Event<QuickStartMySitePrompts>>
    val onQuickStartTabStep = _onQuickStartTabStep as LiveData<QuickStartTabStep?>
    val activeTask = _activeTask as LiveData<QuickStartTask?>
    val isQuickStartNoticeShown = _isQuickStartNoticeShown
    var currentTab = if (isMySiteTabsEnabled) MySiteTabType.DASHBOARD else MySiteTabType.ALL
    val isQuickStartForExistingUsersV2FeatureEnabled = quickStartForExistingUsersV2FeatureConfig.isEnabled()
    var quickStartTaskOriginTab = if (isMySiteTabsEnabled) MySiteTabType.DASHBOARD else MySiteTabType.ALL
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
        clearTabStep()
    }

    fun clearActiveTask() {
        _activeTask.value = null
    }

    fun clearPendingTask() {
        pendingTask = null
    }

    fun clearTabStep() {
        if (_onQuickStartTabStep.value != null) {
            _onQuickStartTabStep.value = null
        }
    }

    fun checkAndSetQuickStartType(isNewSite: Boolean) {
        if (!isQuickStartForExistingUsersV2FeatureEnabled) return
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            val siteLocalId = selectedSite.id.toLong()
            val quickStartType = if (isNewSite) NewSiteQuickStartType else ExistingSiteQuickStartType
            appPrefsWrapper.setLastSelectedQuickStartTypeForSite(quickStartType, siteLocalId)
        }
    }

    suspend fun getQuickStartTaskTypes(siteLocalId: Int): List<QuickStartTaskType> {
        val taskTypes = quickStartType.taskTypes.filterNot { it == UNKNOWN }
        return if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
            dynamicCardStore.getCards(siteLocalId).dynamicCardTypes
                    .filter { dynamicCardType ->
                        dynamicCardType in taskTypes.map { it.toDynamicCardType() }
                    }
                    .map { it.toQuickStartTaskType() }
        } else {
            taskTypes
        }
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

    fun setActiveTask(task: QuickStartTask) {
        _activeTask.postValue(task)
        clearPendingTask()
        clearTabStep()
        when {
            isSiteMenuStepRequiredForTask(task) -> requestTabStepForTask(task, MySiteTabType.SITE_MENU)
            isHomeStepRequiredForTask(task) -> requestTabStepForTask(task, MySiteTabType.DASHBOARD)
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

    fun setTaskDoneAndTrack(
        task: QuickStartTask,
        siteLocalId: Int
    ) {
        quickStartStore.setDoneTask(siteLocalId.toLong(), task, true)
        quickStartTracker.track(quickStartUtilsWrapper.getTaskCompletedTracker(task))
    }

    private fun requestTabStepForTask(task: QuickStartTask, tabType: MySiteTabType) {
        clearActiveTask()
        pendingTask = task
        val shortQuickStartMessage = resourceProvider.getString(
                R.string.quick_start_site_menu_tab_message_short,
                resourceProvider.getString(tabType.stringResId)
        )
        _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(htmlCompat.fromHtml(shortQuickStartMessage)))))
        _onQuickStartTabStep.postValue(QuickStartTabStep(true, task, tabType))
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

    suspend fun onCategoryCompleted(siteLocalId: Int, categoryType: QuickStartTaskType) {
        if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
            val completionMessage = getCategoryCompletionMessage(categoryType)
            _onSnackbar.postValue(Event(SnackbarMessageHolder(UiStringText(completionMessage.asHtml()))))
            dynamicCardStore.removeCard(siteLocalId, categoryType.toDynamicCardType())
        }
    }

    @Suppress("ForbiddenComment")
    private fun getCategoryCompletionMessage(taskType: QuickStartTaskType) = when (taskType) {
        CUSTOMIZE -> R.string.quick_start_completed_type_customize_message
        GROW -> R.string.quick_start_completed_type_grow_message
        // TODO: ashiagr GET_TO_KNOW_APP add message
        GET_TO_KNOW_APP -> R.string.quick_start_completed_type_grow_message
        UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
    }.let { resourceProvider.getString(it) }

    private fun String.asHtml() = htmlCompat.fromHtml(this)

    private fun DynamicCardType.toQuickStartTaskType(): QuickStartTaskType {
        return when (this) {
            CUSTOMIZE_QUICK_START -> CUSTOMIZE
            GROW_QUICK_START -> GROW
            GET_TO_KNOW_APP_QUICK_START -> GET_TO_KNOW_APP
        }
    }

    private fun QuickStartTaskType.toDynamicCardType(): DynamicCardType {
        return when (this) {
            CUSTOMIZE -> CUSTOMIZE_QUICK_START
            GROW -> GROW_QUICK_START
            GET_TO_KNOW_APP -> GET_TO_KNOW_APP_QUICK_START
            UNKNOWN -> throw IllegalArgumentException("Unexpected quick start type")
        }
    }

    fun checkAndShowQuickStartNotice() {
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSite()?.id ?: -1
        if (quickStartType.isQuickStartInProgress(quickStartStore, selectedSiteLocalId.toLong()) &&
                appPrefsWrapper.isQuickStartNoticeRequired()) {
            showQuickStartNotice(selectedSiteLocalId)
        }
    }

    fun showCompletedQuickStartNotice() {
        launch {
            delay(QUICK_START_COMPLETED_NOTICE_DELAY)
            val message = htmlMessageUtils.getHtmlMessageFromStringFormat(
                    "<b>${resourceProvider.getString(R.string.quick_start_completed_tour_title)}</b>" +
                            " ${resourceProvider.getString(R.string.quick_start_completed_tour_subtitle)}"
            )
            _onSnackbar.postValue(Event(
                    SnackbarMessageHolder(
                            message = UiStringText(message),
                            duration = QUICK_START_NOTICE_DURATION,
                            isImportant = false
                    )
            ))
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

    private fun onQuickStartNoticeButtonAction(task: QuickStartTask) {
        quickStartTracker.track(Stat.QUICK_START_TASK_DIALOG_POSITIVE_TAPPED)
        setActiveTask(task)
    }

    private fun onQuickStartNoticeNegativeAction(task: QuickStartTask) {
        quickStartTracker.track(Stat.QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED)
        appPrefsWrapper.setLastSkippedQuickStartTask(task)
    }

    private fun isSiteMenuStepRequiredForTask(task: QuickStartTask) =
            currentTab == MySiteTabType.DASHBOARD && task.isShownInSiteMenuTab()

    private fun isHomeStepRequiredForTask(task: QuickStartTask) =
            quickStartTaskOriginTab == MySiteTabType.DASHBOARD &&
                    currentTab == MySiteTabType.SITE_MENU &&
                    task.isShownInHomeTab()

    // the quick start focus point shown in case of when the default tab is site menu or dashboard varies
    // this function checks whether the passed tasks is shown in site menu
    private fun QuickStartTask.isShownInSiteMenuTab() =
            when (quickStartTaskOriginTab) {
                MySiteTabType.DASHBOARD ->
                    when (this) {
                        QuickStartNewSiteTask.ENABLE_POST_SHARING -> true
                        else -> false
                    }
                MySiteTabType.SITE_MENU ->
                    when (this) {
                        quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL),
                        quickStartType.getTaskFromString(QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL),
                        QuickStartNewSiteTask.REVIEW_PAGES,
                        QuickStartNewSiteTask.ENABLE_POST_SHARING -> true
                        else -> false
                    }
                else -> false
            }

    private fun QuickStartTask.isShownInHomeTab() = when (this) {
        quickStartType.getTaskFromString(QuickStartStore.QUICK_START_CHECK_STATS_LABEL),
        quickStartType.getTaskFromString(QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL),
        QuickStartNewSiteTask.REVIEW_PAGES -> true
        else -> false
    }

    data class QuickStartTabStep(
        val isStarted: Boolean,
        val task: QuickStartTask? = null,
        val mySiteTabType: MySiteTabType
    )

    data class QuickStartCategory(
        val taskType: QuickStartTaskType,
        val uncompletedTasks: List<QuickStartTaskDetails>,
        val completedTasks: List<QuickStartTaskDetails>
    )

    companion object {
        private const val QUICK_START_NOTICE_DURATION = 7000
        private const val QUICK_START_COMPLETED_NOTICE_DELAY = 5000L
    }
}
