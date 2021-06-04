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
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named

class BloggingRemindersViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val bloggingRemindersManager: BloggingRemindersManager,
    private val bloggingRemindersStore: BloggingRemindersStore,
    private val resourceProvider: ResourceProvider,
    private val prologueBuilder: PrologueBuilder,
    private val daySelectionBuilder: DaySelectionBuilder
) : ScopedViewModel(mainDispatcher) {
    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing = _isBottomSheetShowing as LiveData<Event<Boolean>>
    private val _selectedScreen = MutableLiveData<Screen>()
    private val _bloggingRemindersModel = MutableLiveData<BloggingRemindersModel>()
    val uiState: LiveData<UiState> = merge(
            _selectedScreen,
            _bloggingRemindersModel
    ) { screen, bloggingRemindersModel ->
        val uiItems = when (screen) {
            PROLOGUE -> prologueBuilder.buildUiItems()
            SELECTION -> daySelectionBuilder.buildSelection(bloggingRemindersModel, this::selectDay)
            EPILOGUE -> buildEpilogue()
            null -> null
        }
        val primaryButton = when (screen) {
            PROLOGUE -> prologueBuilder.buildPrimaryButton(startDaySelection)
            SELECTION -> daySelectionBuilder.buildPrimaryButton(bloggingRemindersModel, this::showEpilogue)
            EPILOGUE -> buildEpiloguePrimaryButton()
            null -> null
        }
        UiState(uiItems ?: listOf(), primaryButton)
    }.distinctUntilChanged()

    private val startDaySelection: () -> Unit = {
        _selectedScreen.value = SELECTION
    }

    private val finish: () -> Unit = {
        _isBottomSheetShowing.value = Event(false)
    }

    fun getSettingsState(siteId: Int): LiveData<String> {
        return bloggingRemindersStore.bloggingRemindersModel(siteId).map {
            if (it.enabledDays.isNotEmpty()) {
                resourceProvider.getString(
                        R.string.blogging_goals_n_times_a_week,
                        it.enabledDays.size
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

    private fun buildEpilogue(): List<BloggingRemindersItem> {
        // TODO Add epilogue view items
        return listOf()
    }

    private fun buildEpiloguePrimaryButton(): PrimaryButton {
        return PrimaryButton(
                UiStringRes(string.blogging_reminders_done),
                enabled = true,
                ListItemInteraction.create(finish)
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

    data class UiState(val uiItems: List<BloggingRemindersItem>, val primaryButton: PrimaryButton? = null) {
        data class PrimaryButton(val text: UiString, val enabled: Boolean, val onClick: ListItemInteraction)
    }

    companion object {
        private const val SELECTED_SCREEN = "key_shown_screen"
        private const val SELECTED_DAYS = "key_selected_days"
        private const val SITE_ID = "key_site_id"
    }
}
