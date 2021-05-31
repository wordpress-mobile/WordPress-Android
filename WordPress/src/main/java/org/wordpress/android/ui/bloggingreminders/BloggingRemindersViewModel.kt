package org.wordpress.android.ui.bloggingreminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
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
    private val _uiState = MutableLiveData<List<BloggingRemindersItem>>()
    val uiState = _uiState as LiveData<List<BloggingRemindersItem>>

    fun getSettingsState(siteId: Int): LiveData<String> {
        return bloggingRemindersStore.bloggingRemindersModel(siteId).map {
            if (it.enabledDays.isNotEmpty()) {
                resourceProvider.getString(
                        R.string.blogging_goals_n_times_a_week,
                        listOf(UiStringText(it.enabledDays.size.toString()))
                )
            } else {
                resourceProvider.getString(R.string.blogging_goals_not_set)
            }
        }.asLiveData(mainDispatcher)
    }

    fun showBottomSheet(siteId: Int) {
        bloggingRemindersManager.bloggingRemindersShown(siteId)
        _isBottomSheetShowing.value = Event(true)
        _uiState.value = listOf(
                Illustration(R.drawable.img_illustration_stars_130dp),
                Title(UiStringRes(R.string.set_your_blogging_goals_title)),
                Text(UiStringRes(R.string.set_your_blogging_goals_message)),
                PrimaryButton(
                        UiStringRes(R.string.set_your_blogging_goals_button),
                        ListItemInteraction.create(this::onPrimaryClick)
                )
        )
    }

    private fun onPrimaryClick() {
        // TODO handle primary click
    }
}
