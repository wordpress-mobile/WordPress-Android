package org.wordpress.android.ui.accounts.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.WordPress
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class LoginNoSitesErrorViewModel @Inject constructor(
    private val unifiedLoginTracker: UnifiedLoginTracker,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    private var isStarted = false
    fun start(application: WordPress) {
        if (isStarted) return
        isStarted = true

        signOutWordPress(application)
    }

    private fun signOutWordPress(application: WordPress) {
        launch {
            withContext(bgDispatcher) {
                application.wordPressComSignOut()
            }
        }
    }

    fun onFragmentResume() {
        unifiedLoginTracker.track(step = Step.NO_JETPACK_SITES)
    }

    fun onBackPressed() {
        _navigationEvents.postValue(Event(ShowSignInForResultJetpackOnly))
    }
}
