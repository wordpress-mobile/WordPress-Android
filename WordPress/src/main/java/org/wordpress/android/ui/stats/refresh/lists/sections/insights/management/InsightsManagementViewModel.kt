package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class InsightsManagementViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val mutableSnackbarMessage = MutableLiveData<Int>()

    val showSnackbarMessage: LiveData<SnackbarMessageHolder> = mutableSnackbarMessage.map {
        SnackbarMessageHolder(it)
    }

    override fun onCleared() {
        mutableSnackbarMessage.value = null
    }
}
