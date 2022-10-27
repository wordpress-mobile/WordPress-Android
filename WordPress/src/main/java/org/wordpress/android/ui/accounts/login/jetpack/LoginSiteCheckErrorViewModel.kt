package org.wordpress.android.ui.accounts.login.jetpack

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LoginSiteCheckErrorViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    private var isStarted = false
    fun start() {
        if (isStarted) return
        isStarted = true
    }

    fun onSeeInstructionsPressed() {
        _navigationEvents.postValue(Event(ShowInstructions()))
    }

    fun onTryAnotherAccountPressed() {
        _navigationEvents.postValue(Event(ShowSignInForResultJetpackOnly))
    }

    fun onBackPressed() {
        _navigationEvents.postValue(Event(ShowSignInForResultJetpackOnly))
    }
}
