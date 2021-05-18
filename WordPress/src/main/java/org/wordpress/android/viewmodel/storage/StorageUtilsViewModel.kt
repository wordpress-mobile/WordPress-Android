package org.wordpress.android.viewmodel.storage

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.StorageUtilsProvider
import org.wordpress.android.util.StorageUtilsProvider.Source
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class StorageUtilsViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val storageUtilsProvider: StorageUtilsProvider
) : ScopedViewModel(bgDispatcher) {
    private val _checkStorageWarning = MutableLiveData<Event<Unit>>()
    val checkStorageWarning: LiveData<Event<Unit>> = _checkStorageWarning

    fun start(isFirstStart: Boolean) {
        if (!isFirstStart) return

        launch(bgDispatcher) {
            delay(DIALOG_CHECK_DELAY)
            _checkStorageWarning.postValue(Event(Unit))
        }
    }

    fun onStorageWarningCheck(fm: FragmentManager, source: Source) {
        storageUtilsProvider.notifyOnLowStorageSpace(fm, source)
    }

    companion object {
        private const val DIALOG_CHECK_DELAY = 1000L
    }
}
