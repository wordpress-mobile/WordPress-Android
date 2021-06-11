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
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.EPILOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.PROLOGUE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.Screen.SELECTION
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named

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
    val uiState: LiveData<List<BloggingRemindersItem>> = merge(
            _selectedScreen,
            _bloggingRemindersModel
    ) { screen, bloggingRemindersModel ->
        when (screen) {
            PROLOGUE -> buildPrologue()
            SELECTION -> buildSelection(bloggingRemindersModel)
            EPILOGUE -> buildEpilogue()
            null -> null
        }
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
                    ListItemInteraction.create(startDaySelection)
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

    private fun buildEpilogue(): List<BloggingRemindersItem> {
        // TODO Add epilogue view items
        return listOf(
                PrimaryButton(
                        UiStringRes(R.string.blogging_reminders_done),
                        enabled = true,
                        ListItemInteraction.create(finish)
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

    companion object {
        private const val SELECTED_SCREEN = "key_shown_screen"
        private const val SELECTED_DAYS = "key_selected_days"
        private const val SITE_ID = "key_site_id"
    }
}
