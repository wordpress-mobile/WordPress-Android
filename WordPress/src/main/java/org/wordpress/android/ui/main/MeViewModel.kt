package org.wordpress.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.WordPress
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class MeViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
) : ScopedViewModel(mainDispatcher) {
    private val _showDisconnectDialog = MutableLiveData<Event<Boolean>>()
    val showDisconnectDialog: LiveData<Event<Boolean>> = _showDisconnectDialog

    private val _showUnifiedAbout = MutableLiveData<Event<Boolean>>()
    val showUnifiedAbout: LiveData<Event<Boolean>> = _showUnifiedAbout

    fun signOutWordPress(application: WordPress) {
        launch {
            _showDisconnectDialog.value = Event(true)
            withContext(bgDispatcher) {
                application.wordPressComSignOut()
            }
            _showDisconnectDialog.value = Event(false)
        }
    }

    fun openDisconnectDialog() {
        _showDisconnectDialog.value = Event(true)
    }

    fun getSite() = selectedSiteRepository.getSelectedSite()

    fun showUnifiedAbout() {
        _showUnifiedAbout.value = Event(true)
    }
}
