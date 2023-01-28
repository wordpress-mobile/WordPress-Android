package org.wordpress.android.ui.bloggingreminders

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.BLOGGING_PROMPTS_ONBOARDING
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.BLOG_SETTINGS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.NOTIFICATION_SETTINGS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAnalyticsTracker.Source.PUBLISH_FLOW
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.merge
import org.wordpress.android.util.perform
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.workers.reminder.ReminderScheduler
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Named

class BloggingRemindersViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val bloggingRemindersManager: BloggingRemindersManager,
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val prologueBuilder: PrologueBuilder,
    private val daySelectionBuilder: DaySelectionBuilder,
    private val epilogueBuilder: EpilogueBuilder,
    private val dayLabelUtils: DayLabelUtils,
    private val analyticsTracker: BloggingRemindersAnalyticsTracker,
    private val reminderScheduler: ReminderScheduler,
    private val mapper: BloggingRemindersModelMapper,
    private val siteStore: SiteStore
) : ScopedViewModel(mainDispatcher) {
    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing = _isBottomSheetShowing as LiveData<Event<Boolean>>

    private val _isTimePickerShowing = MutableLiveData<Event<Boolean>>()
    val isTimePickerShowing = _isTimePickerShowing as LiveData<Event<Boolean>>

    private val _showBloggingPromptHelpDialogVisible = MutableLiveData<Event<Boolean>>()
    val showBloggingPromptHelpDialogVisible = _showBloggingPromptHelpDialogVisible as LiveData<Event<Boolean>>

    private val _selectedScreen = MutableLiveData<Screen>()
    private val selectedScreen = _selectedScreen.perform { onScreenChanged(it) }

    private val _bloggingRemindersModel = MutableLiveData<BloggingRemindersUiModel>()
    private val _isFirstTimeFlow = MutableLiveData<Boolean>()

    val uiState: LiveData<UiState> = merge(
        selectedScreen,
        _bloggingRemindersModel,
        _isFirstTimeFlow
    ) { screen, bloggingRemindersModel, isFirstTimeFlow ->
        if (screen != null) {
            val uiItems = when (screen) {
                Screen.PROLOGUE -> prologueBuilder.buildUiItems()
                Screen.PROLOGUE_SETTINGS -> prologueBuilder.buildUiItemsForSettings()
                Screen.SELECTION -> daySelectionBuilder.buildSelection(
                    bloggingRemindersModel,
                    this::selectDay,
                    this::selectTime,
                    this::togglePromptSwitch,
                    this::showBloggingPromptDialog
                )
                Screen.EPILOGUE -> epilogueBuilder.buildUiItems(bloggingRemindersModel)
            }
            val primaryButton = when (screen) {
                Screen.PROLOGUE, Screen.PROLOGUE_SETTINGS -> prologueBuilder.buildPrimaryButton(
                    isFirstTimeFlow == true,
                    startDaySelection
                )
                Screen.SELECTION -> daySelectionBuilder.buildPrimaryButton(
                    bloggingRemindersModel,
                    isFirstTimeFlow == true,
                    this::showEpilogue
                )
                Screen.EPILOGUE -> epilogueBuilder.buildPrimaryButton(finish)
            }
            UiState(uiItems, primaryButton)
        } else {
            UiState(listOf())
        }
    }.distinctUntilChanged()

    private val startDaySelection: (isFirstTimeFlow: Boolean) -> Unit = { isFirstTimeFlow ->
        analyticsTracker.trackPrimaryButtonPressed(Screen.PROLOGUE)
        _isFirstTimeFlow.value = isFirstTimeFlow
        _selectedScreen.value = Screen.SELECTION
    }

    private val finish: () -> Unit = {
        analyticsTracker.trackPrimaryButtonPressed(Screen.EPILOGUE)
        _isBottomSheetShowing.value = Event(false)
    }

    private fun onScreenChanged(screen: Screen) {
        analyticsTracker.trackScreenShown(screen)
    }

    fun getBlogSettingsUiState(siteId: Int) = getUiModel(siteId)
        .map { dayLabelUtils.buildSiteSettingsLabel(it) }
        .asLiveData(mainDispatcher)

    val notificationsSettingsUiState = combine(siteStore.sites.map { getUiModel(it.id) }) { models ->
        models.associate { siteStore.getSiteIdForLocalId(it.siteId) to dayLabelUtils.buildSiteSettingsLabel(it) }
    }.asLiveData(mainDispatcher)

    private fun getUiModel(siteId: Int) =
        bloggingRemindersStore.bloggingRemindersModel(siteId).map { mapper.toUiModel(it) }

    private fun showBottomSheet(siteId: Int, screen: Screen, source: Source) {
        analyticsTracker.setSite(siteId)
        analyticsTracker.trackFlowStart(source)
        val isPrologueScreen = screen == Screen.PROLOGUE || screen == Screen.PROLOGUE_SETTINGS
        if (isPrologueScreen) {
            bloggingRemindersManager.bloggingRemindersShown(siteId)
        }
        _isFirstTimeFlow.value = isPrologueScreen
        _isBottomSheetShowing.value = Event(true)
        _selectedScreen.value = screen
        launch {
            bloggingRemindersStore.bloggingRemindersModel(siteId).collect {
                _bloggingRemindersModel.value = mapper.toUiModel(it)
            }
        }
    }

    fun selectDay(day: DayOfWeek) {
        val currentState = _bloggingRemindersModel.value!!
        val enabledDays = currentState.enabledDays.toMutableSet()
        if (enabledDays.contains(day)) {
            enabledDays.remove(day)
        } else {
            enabledDays.add(day)
        }
        _bloggingRemindersModel.value = currentState.copy(enabledDays = enabledDays)
    }

    fun selectTime() {
        _isTimePickerShowing.value = Event(true)
    }

    private fun togglePromptSwitch() {
        _bloggingRemindersModel.value?.let { currentState ->
            val newState = !currentState.isPromptIncluded
            analyticsTracker.trackRemindersIncludePromptPressed(newState)
            _bloggingRemindersModel.value = currentState.copy(isPromptIncluded = newState)
        }
    }

    private fun showBloggingPromptDialog() {
        analyticsTracker.trackRemindersIncludePromptHelpPressed()
        _showBloggingPromptHelpDialogVisible.value = Event(true)
    }

    fun onChangeTime(hour: Int, minute: Int) {
        _isTimePickerShowing.value = Event(false)
        val currentState = _bloggingRemindersModel.value!!
        _bloggingRemindersModel.value = currentState.copy(hour = hour, minute = minute)
    }

    fun getSelectedTime(): Pair<Int, Int> {
        val currentState = _bloggingRemindersModel.value!!
        return Pair(currentState.hour, currentState.minute)
    }

    private fun showEpilogue(bloggingRemindersModel: BloggingRemindersUiModel?) {
        analyticsTracker.trackPrimaryButtonPressed(Screen.SELECTION)
        if (bloggingRemindersModel != null) {
            launch {
                bloggingRemindersStore.updateBloggingReminders(
                    mapper.toDomainModel(
                        bloggingRemindersModel
                    )
                )
                val daysCount = bloggingRemindersModel.enabledDays.size
                if (daysCount > 0) {
                    reminderScheduler.schedule(
                        bloggingRemindersModel.siteId,
                        bloggingRemindersModel.hour,
                        bloggingRemindersModel.minute,
                        bloggingRemindersModel.toReminderConfig()
                    )
                    analyticsTracker.trackRemindersScheduled(
                        daysCount, bloggingRemindersModel.getNotificationTime24hour()
                    )
                } else {
                    reminderScheduler.cancelBySiteId(bloggingRemindersModel.siteId)
                    analyticsTracker.trackRemindersCancelled()
                }
                _selectedScreen.value = Screen.EPILOGUE
            }
        }
    }

    fun saveState(outState: Bundle) {
        _selectedScreen.value?.let {
            outState.putSerializable(SELECTED_SCREEN, it)
        }
        _bloggingRemindersModel.value?.let { model ->
            outState.putInt(SITE_ID, model.siteId)
            outState.putStringArrayList(SELECTED_DAYS, ArrayList(model.enabledDays.map { it.name }))
            outState.putInt(SELECTED_HOUR, model.hour)
            outState.putInt(SELECTED_MINUTE, model.minute)
            outState.putBoolean(IS_BLOGGING_PROMPT_INCLUDED, model.isPromptIncluded)
        }
        _isFirstTimeFlow.value?.let {
            outState.putBoolean(IS_FIRST_TIME_FLOW, it)
        }
    }

    fun restoreState(state: Bundle) {
        state.getSerializableCompat<Screen>(SELECTED_SCREEN)?.let {
            _selectedScreen.value = it
        }
        val siteId = state.getInt(SITE_ID)
        if (siteId != 0) {
            val enabledDays = state.getStringArrayList(SELECTED_DAYS)?.map { DayOfWeek.valueOf(it) }?.toSet() ?: setOf()
            val selectedHour = state.getInt(SELECTED_HOUR)
            val selectedMinute = state.getInt(SELECTED_MINUTE)
            val isPromptIncluded = state.getBoolean(IS_BLOGGING_PROMPT_INCLUDED)
            _bloggingRemindersModel.value = BloggingRemindersUiModel(
                siteId,
                enabledDays,
                selectedHour,
                selectedMinute,
                isPromptIncluded
            )
        }
        _isFirstTimeFlow.value = state.getBoolean(IS_FIRST_TIME_FLOW)
    }

    fun onPublishingPost(siteId: Int, isFirstTimePublishing: Boolean?) {
        if (isFirstTimePublishing == true && bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)) {
            showBottomSheet(siteId, Screen.PROLOGUE, PUBLISH_FLOW)
        }
    }

    fun onBlogSettingsItemClicked(siteId: Int) {
        onSettingsItemClicked(siteId, BLOG_SETTINGS)
    }

    fun onNotificationSettingsItemClicked(remoteSiteId: Long) {
        onSettingsItemClicked(siteStore.getLocalIdForRemoteSiteId(remoteSiteId), NOTIFICATION_SETTINGS)
    }

    fun onBloggingPromptSchedulingRequested(siteId: Int) {
        showBottomSheet(siteId, Screen.PROLOGUE, BLOGGING_PROMPTS_ONBOARDING)
    }

    private fun onSettingsItemClicked(siteId: Int, source: Source) {
        launch {
            val screen = if (bloggingRemindersStore.hasModifiedBloggingReminders(siteId)) {
                Screen.SELECTION
            } else {
                Screen.PROLOGUE_SETTINGS
            }
            showBottomSheet(siteId, screen, source)
        }
    }

    fun onBottomSheetDismissed() {
        when (val screen = selectedScreen.value) {
            Screen.PROLOGUE,
            Screen.PROLOGUE_SETTINGS,
            Screen.SELECTION -> analyticsTracker.trackFlowDismissed(screen)
            Screen.EPILOGUE -> analyticsTracker.trackFlowCompleted()
            null -> Unit // Do nothing
        }
    }

    enum class Screen(val trackingName: String) {
        PROLOGUE("main"), // displayed after post is published
        PROLOGUE_SETTINGS("main"), // displayed from Site Settings before showing cadence selector
        SELECTION("day_picker"), // cadence selector
        EPILOGUE("all_set")
    }

    data class UiState(
        val uiItems: List<BloggingRemindersItem>,
        val primaryButton: PrimaryButton? = null
    ) {
        data class PrimaryButton(val text: UiString, val enabled: Boolean, val onClick: ListItemInteraction)
    }

    companion object {
        private const val SELECTED_SCREEN = "key_shown_screen"
        private const val SELECTED_DAYS = "key_selected_days"
        private const val SELECTED_HOUR = "key_selected_hour"
        private const val SELECTED_MINUTE = "key_selected_minute"
        private const val IS_BLOGGING_PROMPT_INCLUDED = "key_is_blogging_prompt_included"
        private const val IS_FIRST_TIME_FLOW = "is_first_time_flow"
        private const val SITE_ID = "key_site_id"
    }
}
