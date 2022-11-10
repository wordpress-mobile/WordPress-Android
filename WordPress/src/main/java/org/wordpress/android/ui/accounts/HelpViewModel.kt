package org.wordpress.android.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.WordPress
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HelpViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {
    private val _onSignOutCompleted = SingleLiveEvent<Unit>()
    val onSignedOutCompleted: LiveData<Unit> = _onSignOutCompleted

    fun signOutWordPress(application: WordPress) {
        launch {
            withContext(bgDispatcher) {
                application.wordPressComSignOut()
            }
            _onSignOutCompleted.call()
        }
    }
}
