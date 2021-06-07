package org.wordpress.android.ui.bloggingreminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BloggingRemindersViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing = _isBottomSheetShowing as LiveData<Event<Boolean>>
    private val _uiState = MutableLiveData<List<BloggingRemindersItem>>()
    val uiState = _uiState as LiveData<List<BloggingRemindersItem>>

    fun start() {
        _isBottomSheetShowing.value = Event(true)
        _uiState.value = listOf(
//            Title(UiStringText("Set your blogging goals")),
        )
    }
}
