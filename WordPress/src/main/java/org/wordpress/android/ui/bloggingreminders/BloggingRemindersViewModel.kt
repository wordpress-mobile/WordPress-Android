package org.wordpress.android.ui.bloggingreminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.CloseButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BloggingRemindersViewModel @Inject constructor(
    private val bloggingRemindersManager: BloggingRemindersManager,
    private val bloggingRemindersStore: BloggingRemindersStore,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing = _isBottomSheetShowing as LiveData<Event<Boolean>>
    private val _uiState = MutableLiveData<List<BloggingRemindersItem>>()
    val uiState = _uiState as LiveData<List<BloggingRemindersItem>>

    fun getSettingsState(siteId: Int): LiveData<String> {
        return bloggingRemindersStore.bloggingRemindersModel(siteId).map {
            if (it.enabledDays.isNotEmpty()) {
                "Blogging reminders ${it.enabledDays.size} times a week"
            } else {
                "No blogging reminders set"
            }
        }.asLiveData(mainDispatcher)
    }

    fun showBottomSheet(siteId: Int) {
        bloggingRemindersManager.bloggingRemindersShown(siteId)
        _isBottomSheetShowing.value = Event(true)
        _uiState.value = listOf(
                CloseButton(ListItemInteraction.create(this::onClose)),
                // TODO update with actual illustration
                Illustration(R.drawable.img_illustration_cloud_off_152dp),
                // TODO update with actual copy
                Title(UiStringText("Set your blogging goals!")),
                // TODO update with actual copy
                Text(UiStringText("Well done on your first post! Keep it going.")),
                PrimaryButton(UiStringRes(R.string.get_started), ListItemInteraction.create(this::onPrimaryClick))
        )
    }

    private fun onClose() {
        _isBottomSheetShowing.value = Event(false)
    }

    private fun onPrimaryClick() {
        // TODO handle primary click
    }
}
