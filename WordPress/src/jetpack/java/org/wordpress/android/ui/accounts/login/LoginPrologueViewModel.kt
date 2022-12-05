package org.wordpress.android.ui.accounts.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowEmailLoginScreen
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowLoginViaSiteAddressScreen
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click.CONTINUE_WITH_WORDPRESS_COM
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click.LOGIN_WITH_SITE_ADDRESS
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step.PROLOGUE
import org.wordpress.android.ui.accounts.login.LoginPrologueViewModel.ButtonUiState.ContinueWithWpcomButtonState
import org.wordpress.android.ui.accounts.login.LoginPrologueViewModel.ButtonUiState.EnterYourSiteAddressButtonState
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LoginPrologueViewModel @Inject constructor(
    private val unifiedLoginTracker: UnifiedLoginTracker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private var isStarted = false
    fun start() {
        if (isStarted) return
        isStarted = true

        analyticsTrackerWrapper.track(stat = Stat.LOGIN_PROLOGUE_VIEWED)
        unifiedLoginTracker.track(flow = Flow.PROLOGUE, step = PROLOGUE)

        _uiState.value = UiState(
                enterYourSiteAddressButtonState = EnterYourSiteAddressButtonState(::onEnterYourSiteAddressButtonClick),
                continueWithWpcomButtonState = ContinueWithWpcomButtonState(
                        title = if (buildConfigWrapper.isSignupEnabled) {
                            R.string.continue_with_wpcom
                        } else {
                            R.string.continue_with_wpcom_no_signup
                        },
                        onClick = ::onContinueWithWpcomButtonClick
                )
        )
    }

    fun onFragmentResume() {
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, PROLOGUE)
    }

    private fun onContinueWithWpcomButtonClick() {
        unifiedLoginTracker.trackClick(CONTINUE_WITH_WORDPRESS_COM)
        _navigationEvents.postValue(Event(ShowEmailLoginScreen))
    }

    private fun onEnterYourSiteAddressButtonClick() {
        unifiedLoginTracker.trackClick(LOGIN_WITH_SITE_ADDRESS)
        _navigationEvents.postValue(Event(ShowLoginViaSiteAddressScreen))
    }

    data class UiState(
        val enterYourSiteAddressButtonState: EnterYourSiteAddressButtonState,
        val continueWithWpcomButtonState: ContinueWithWpcomButtonState
    )

    sealed class ButtonUiState {
        abstract val title: Int
        abstract val onClick: (() -> Unit)

        data class ContinueWithWpcomButtonState(
            override val title: Int,
            override val onClick: () -> Unit
        ) : ButtonUiState()

        data class EnterYourSiteAddressButtonState(override val onClick: () -> Unit) : ButtonUiState() {
            override val title = R.string.enter_your_site_address
        }
    }
}
