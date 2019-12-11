package org.wordpress.android.ui.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderCommentListViewModel
@Inject constructor(@Named(UI_THREAD) mainDispatcher: CoroutineDispatcher) : ScopedViewModel(
        mainDispatcher
) {
    private val _scrollTo = MutableLiveData<Event<ScrollPosition>>()
    val scrollTo: LiveData<Event<ScrollPosition>> = _scrollTo.distinct()

    private var scrollJob: Job? = null

    fun scrollToPosition(position: Int, isSmooth: Boolean) {
        scrollJob?.cancel()
        scrollJob = launch {
            delay(300)
            _scrollTo.postValue(Event(ScrollPosition(position, isSmooth)))
        }
    }

    data class ScrollPosition(val position: Int, val isSmooth: Boolean)
}
