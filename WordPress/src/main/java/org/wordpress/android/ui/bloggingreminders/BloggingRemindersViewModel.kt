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
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@Suppress("TooManyFunctions")
class BloggingRemindersViewModel @Inject constructor(
    private val bloggingRemindersManager: BloggingRemindersManager,
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val resourceProvider: ResourceProvider,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing = _isBottomSheetShowing as LiveData<Event<Boolean>>
    private val _selectedScreen = MutableLiveData<Screen>()
    private val _bloggingRemindersModel = MutableLiveData<BloggingRemindersModel>()
    @ExperimentalStdlibApi val uiState: LiveData<List<BloggingRemindersItem>> = merge(
            _selectedScreen,
            _bloggingRemindersModel
    ) { screen, bloggingRemindersModel ->
        when (screen) {
            PROLOGUE -> buildPrologue()
            SELECTION -> buildSelection(bloggingRemindersModel)
            EPILOGUE -> buildEpilogue(bloggingRemindersModel)
            null -> null
        }
    }.distinctUntilChanged()

    fun getSettingsState(siteId: Int): LiveData<String> {
        return bloggingRemindersStore.bloggingRemindersModel(siteId).map {
            if (it.enabledDays.isNotEmpty()) {
                resourceProvider.getString(
                        R.string.blogging_goals_n_times_a_week,
                        UiStringText(it.enabledDays.size.toString())
                )
            } else {
                resourceProvider.getString(R.string.blogging_goals_not_set)
            }
        }.asLiveData(mainDispatcher)
    }

    fun showBottomSheet(siteId: Int, screen: Screen) {
        if (screen == PROLOGUE) {
            bloggingRemindersManager.bloggingRemindersShown(siteId)
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
            Text(UiStringRes(R.string.set_your_blogging_goals_message)),
            PrimaryButton(
                    UiStringRes(R.string.set_your_blogging_goals_button),
                    enabled = true,
                    ListItemInteraction.create(this::startDaySelection)
            )
    )

    private fun buildSelection(bloggingRemindersModel: BloggingRemindersModel?): List<BloggingRemindersItem> {
        // TODO Add selection view items
        return listOf(
                PrimaryButton(
                        UiStringRes(R.string.blogging_reminders_notify_me),
                        enabled = bloggingRemindersModel?.enabledDays?.isNotEmpty() == true,
                        ListItemInteraction.create(bloggingRemindersModel, this::showEpilogue)
                )
        )
    }

    @ExperimentalStdlibApi
    private fun buildEpilogue(bloggingRemindersModel: BloggingRemindersModel?): List<BloggingRemindersItem> {
        val numberOfDays = bloggingRemindersModel?.enabledDays?.size ?: 0

        val selectedDays = when {
            numberOfDays <= THREE_DAYS -> {
                bloggingRemindersModel?.enabledDays?.map { day ->
                    day.name.toLowerCase(Locale.ROOT).replaceFirstChar {
                        it.toUpperCase()
                    }.plus(" and ")
                }.toString()
            }
            numberOfDays in FOUR_DAYS..SIX_DAYS -> {
                bloggingRemindersModel?.enabledDays?.map { day ->
                    day.name.toLowerCase(Locale.ROOT).replaceFirstChar {
                        it.toUpperCase()
                    }.take(FIRST_THREE_CHARS).plus(", ")
                }.toString()
            }
            else -> "everyday"
        }

        val body = when (numberOfDays) {
            SEVEN_DAYS -> UiStringRes(R.string.blogging_reminders_epilogue_body)
            else -> UiStringResWithParams(
                    R.string.blogging_reminders_epilogue_body_days,
                    listOf(UiStringText(numberOfDays.toString()), UiStringText(selectedDays)))
        }

        return listOf(
                Illustration(R.drawable.img_illustration_bell_yellow_96dp),
                Title(UiStringRes(R.string.blogging_reminders_epilogue_title)),
                Text(body),
                Caption(UiStringRes(R.string.blogging_reminders_epilogue_caption)),
                PrimaryButton(
                        UiStringRes(R.string.blogging_reminders_done),
                        enabled = true,
                        ListItemInteraction.create(this::finish)
                )
        )
    }

    // TODO Call this method on day selection
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

    private fun startDaySelection() {
        _selectedScreen.value = SELECTION
    }

    private fun showEpilogue(bloggingRemindersModel: BloggingRemindersModel?) {
        if (bloggingRemindersModel != null) {
            // TODO: Perform this update login on coroutine
            // bloggingRemindersStore.updateBloggingReminders(bloggingRemindersModel)
            // TODO Add logic to save state and schedule notifications here
            _selectedScreen.value = EPILOGUE
        }
    }

    private fun finish() {
        _isBottomSheetShowing.value = Event(false)
    }

    fun saveState(outState: Bundle) {
        _selectedScreen.value?.let {
            outState.putSerializable(SELECTED_SCREEN, it)
        }
        _bloggingRemindersModel.value?.let { model ->
            outState.putInt(SITE_ID, model.siteId)
            outState.putStringArrayList(SELECTED_DAYS, ArrayList(model.enabledDays.map { it.name }))
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
    }

    enum class Screen {
        PROLOGUE, SELECTION, EPILOGUE
    }

    companion object {
        private const val SELECTED_SCREEN = "key_shown_screen"
        private const val SELECTED_DAYS = "key_selected_days"
        private const val SITE_ID = "key_site_id"
        private const val FIRST_THREE_CHARS = 3
        private const val THREE_DAYS = 3
        private const val FOUR_DAYS = 4
        private const val SIX_DAYS = 6
        private const val SEVEN_DAYS = 7
    }
}
