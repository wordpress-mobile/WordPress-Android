package org.wordpress.android.ui.bloggingreminders

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.time.DayOfWeek
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@Suppress("TooManyFunctions")
class BloggingRemindersViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val bloggingRemindersManager: BloggingRemindersManager,
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val resourceProvider: ResourceProvider,
    private val prologueBuilder: PrologueBuilder,
    private val daySelectionBuilder: DaySelectionBuilder,
    private val dayLabelUtils: DayLabelUtils
) : ScopedViewModel(mainDispatcher) {
    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing = _isBottomSheetShowing as LiveData<Event<Boolean>>
    private val _selectedScreen = MutableLiveData<Screen>()
    private val _bloggingRemindersModel = MutableLiveData<BloggingRemindersModel>()
    private val _isFirstTimeFlow = MutableLiveData<Boolean>()
    val uiState: LiveData<UiState> = merge(
            _selectedScreen,
            _bloggingRemindersModel,
            _isFirstTimeFlow
    ) { screen, bloggingRemindersModel, isFirstTimeFlow ->
        if (screen != null) {
            val uiItems = when (screen) {
                PROLOGUE -> prologueBuilder.buildUiItems()
                SELECTION -> daySelectionBuilder.buildSelection(bloggingRemindersModel, this::selectDay)
                EPILOGUE -> buildEpilogue()
            }
            val primaryButton = when (screen) {
                PROLOGUE -> prologueBuilder.buildPrimaryButton(startDaySelection)
                SELECTION -> daySelectionBuilder.buildPrimaryButton(
                        bloggingRemindersModel,
                        isFirstTimeFlow == true,
                        this::showEpilogue
                )
                EPILOGUE -> buildEpiloguePrimaryButton()
            }
            UiState(uiItems, primaryButton)
        } else {
            UiState(listOf())
        }
    }.distinctUntilChanged()

    private val startDaySelection: () -> Unit = {
        _isFirstTimeFlow.value = true
        _selectedScreen.value = SELECTION
    }

    private val finish: () -> Unit = {
        _isBottomSheetShowing.value = Event(false)
    }

    private var isFromSiteSettings: Boolean = false

    fun getSettingsState(siteId: Int): LiveData<UiString> {
        return bloggingRemindersStore.bloggingRemindersModel(siteId).map {
            dayLabelUtils.buildNTimesLabel(it)
        }.asLiveData(mainDispatcher)
    }

    fun showBottomSheet(siteId: Int, screen: Screen, fromSiteSettings: Boolean) {
        isFromSiteSettings = fromSiteSettings
        if (screen == PROLOGUE) {
            bloggingRemindersManager.bloggingRemindersShown(siteId)
        } else {
            _isFirstTimeFlow.value = false
        }
        _isBottomSheetShowing.value = Event(true)
        _selectedScreen.value = screen
        launch {
            bloggingRemindersStore.bloggingRemindersModel(siteId).collect {
                _bloggingRemindersModel.value = it
            }
        }
    }

    private fun buildPrologue() = listOf(
            Illustration(R.drawable.img_illustration_celebration_150dp),
            Title(UiStringRes(R.string.set_your_blogging_goals_title)),
            if (!isFromSiteSettings) {
                HighEmphasisText(UiStringRes(R.string.set_your_blogging_goals_message))
            } else {
                HighEmphasisText(UiStringText("")) },
            PrimaryButton(
                    UiStringRes(R.string.set_your_blogging_goals_button),
                    enabled = true,
                    ListItemInteraction.create(startDaySelection)
            )
    )

    private fun buildEpilogue(): List<BloggingRemindersItem> {
        val numberOfTimes = dayLabelUtils.buildNTimesLabel(_bloggingRemindersModel.value)
        val enabledDays = _bloggingRemindersModel.value?.let { model ->
            model.enabledDays.map { DayOfWeek.valueOf(it.name) }
        }

        // TODO: Format number of times and selected days properly after PRs leading up to this are merged
        val selectedDays = when (enabledDays?.count()) {
            ONE_DAY -> enabledDays.joinToString { formattedDay(it) }
            TWO_DAYS -> enabledDays.joinToString(separator = " and ") { formattedDay(it) }
            in THREE_DAYS..SIX_DAYS -> {
                val firstDays = enabledDays?.dropLast(1)
                val lastDay = enabledDays?.drop(enabledDays.count() - 1)
                firstDays?.joinToString(postfix = " and ") {
                    formattedDay(it)
                } + lastDay?.joinToString { formattedDay(it) }
            }
            else -> UiStringRes(R.string.blogging_reminders_epilogue_body_everyday).toString()
        }

        val body = when (enabledDays?.count()) {
            ZERO -> UiStringRes(R.string.blogging_reminders_epilogue_body_no_reminders)
            SEVEN_DAYS -> UiStringRes(R.string.blogging_reminders_epilogue_body_everyday)
            else -> UiStringResWithParams(
                    R.string.blogging_reminders_epilogue_body_days,
                    listOf(numberOfTimes, UiStringText(selectedDays)))
        }

        return listOf(
                Illustration(R.drawable.img_illustration_bell_yellow_96dp),
                Title(UiStringRes(R.string.blogging_reminders_epilogue_title)),
                HighEmphasisText(body),
                Caption(UiStringRes(R.string.blogging_reminders_epilogue_caption))
        )
    }

    private fun formattedDay(it: DayOfWeek) =
            it.name.toLowerCase(Locale.getDefault()) // TODO: Capitalize first letter

    private fun buildEpiloguePrimaryButton(): PrimaryButton {
        return PrimaryButton(
                UiStringRes(R.string.blogging_reminders_done),
                enabled = true,
                ListItemInteraction.create(finish)
        )
    }

    fun selectDay(day: Day) {
        val currentState = _bloggingRemindersModel.value!!
        val enabledDays = currentState.enabledDays.toMutableSet()
        if (enabledDays.contains(day)) {
            enabledDays.remove(day)
        } else {
            enabledDays.add(day)
        }
        _bloggingRemindersModel.value = currentState.copy(enabledDays = enabledDays)
    }

    private fun showEpilogue(bloggingRemindersModel: BloggingRemindersModel?) {
        if (bloggingRemindersModel != null) {
            launch {
                bloggingRemindersStore.updateBloggingReminders(bloggingRemindersModel)
                // TODO Add logic to save state and schedule notifications here
                _selectedScreen.value = EPILOGUE
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
        }
        _isFirstTimeFlow.value?.let {
            outState.putBoolean(IS_FIRST_TIME_FLOW, it)
        }
    }

    fun restoreState(state: Bundle) {
        state.getSerializable(SELECTED_SCREEN)?.let {
            _selectedScreen.value = it as Screen
        }
        val siteId = state.getInt(SITE_ID)
        if (siteId != 0) {
            val enabledDays = state.getStringArrayList(SELECTED_DAYS)?.map { Day.valueOf(it) }?.toSet() ?: setOf()
            _bloggingRemindersModel.value = BloggingRemindersModel(siteId, enabledDays)
        }
        _isFirstTimeFlow.value = state.getBoolean(IS_FIRST_TIME_FLOW)
    }

    fun onPostCreated(siteId: Int, isNewPost: Boolean?) {
        if (isNewPost == true && bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)) {
            showBottomSheet(siteId, PROLOGUE, false)
        }
    }

    enum class Screen {
        PROLOGUE, SELECTION, EPILOGUE
    }

    data class UiState(val uiItems: List<BloggingRemindersItem>, val primaryButton: PrimaryButton? = null) {
        data class PrimaryButton(val text: UiString, val enabled: Boolean, val onClick: ListItemInteraction)
    }

    companion object {
        private const val SELECTED_SCREEN = "key_shown_screen"
        private const val SELECTED_DAYS = "key_selected_days"
        private const val IS_FIRST_TIME_FLOW = "is_first_time_flow"
        private const val SITE_ID = "key_site_id"
        private const val ZERO = 0
        private const val ONE_DAY = 1
        private const val TWO_DAYS = 2
        private const val THREE_DAYS = 3
        private const val SIX_DAYS = 6
        private const val SEVEN_DAYS = 7
    }
}
