package org.wordpress.android.ui.main.emailverificationbanner

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class EmailVerificationViewModel
@Inject constructor(
    @Named(UI_THREAD) val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _verificationState = MutableStateFlow<VerificationState?>(null)
    val verificationState = _verificationState.asStateFlow()

    private val _emailAddress = MutableStateFlow("")
    val emailAddress = _emailAddress.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null

    enum class VerificationState {
        NO_ACCOUNT,     // user doesn't have an account so verification is not possible
        UNVERIFIED,     // user has not verified their email
        LINK_REQUESTED, // user has requested a verification link (API call in progress)
        LINK_SENT,      // verification link has been sent successfully (API call completed)
        LINK_ERROR,     // an error occurred requesting the verification link
        VERIFIED,       // user has verified their email address
    }

    init {
        dispatcher.register(this)
        _emailAddress.value = accountStore.account.email

        _verificationState.value = if (accountStore.account.emailVerified) {
            VerificationState.VERIFIED
        } else if (_emailAddress.value.isNotEmpty()) {
            VerificationState.UNVERIFIED
        } else {
            VerificationState.NO_ACCOUNT
        }
    }

    /**
     * User clicked the "Send verification link" button on the email verification banner
     */
    fun onSendVerificationLinkClick() {
        if (!NetworkUtils.checkConnection(WordPress.getContext())) {
            appLogWrapper.d(AppLog.T.MAIN, "$TAG: No connection")
            return
        }

        if (pollingJob?.isActive == true) {
            appLogWrapper.d(AppLog.T.MAIN, "$TAG: Cancelling verification state polling")
            pollingJob?.cancel()
        }

        _verificationState.value = VerificationState.LINK_REQUESTED
        appLogWrapper.d(AppLog.T.MAIN, "$TAG: Verification link requested")

        // briefly delay the request so the user can see the updated banner if the request completes quickly
        launch(bgDispatcher) {
            delay(REQUEST_LINK_DELAY)
            dispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction())
        }
    }

    /**
     * Repeatedly fetches the user's account to detect when the user has verified their email address
     */
    private fun pollVerificationState() {
        if (pollingJob?.isActive == true) {
            appLogWrapper.w(AppLog.T.MAIN, "$TAG: Already polling verification state")
            return
        }

        pollingJob = launch(bgDispatcher) {
            repeat(POLLING_COUNT) {
                dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
                delay(POLLING_INTERVAL_MS)
                if (accountStore.account.emailVerified) {
                    withContext(mainDispatcher) {
                        _verificationState.value = VerificationState.VERIFIED
                        appLogWrapper.d(AppLog.T.MAIN, "$TAG: Email verified")
                        pollingJob?.cancel()
                        return@withContext
                    }
                }
            }
        }
    }

    /**
     * FluxC event for when the account state changes
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            if (event.error.type == AccountErrorType.SEND_VERIFICATION_EMAIL_ERROR) {
                _errorMessage.value = event.error.message
                _verificationState.value = VerificationState.LINK_ERROR
                appLogWrapper.e(AppLog.T.MAIN, "$TAG: Error sending verification link, ${event.error.message}")
            }
        } else if (event.causeOfChange == AccountAction.SEND_VERIFICATION_EMAIL ||
            event.causeOfChange == AccountAction.SENT_VERIFICATION_EMAIL
        ) {
            if (_verificationState.value != VerificationState.LINK_SENT) {
                _verificationState.value = VerificationState.LINK_SENT
                appLogWrapper.d(AppLog.T.MAIN, "$TAG: Verification link sent")
                pollVerificationState()
            }
        }
    }

    companion object {
        private const val TAG = "EmailVerificationViewModel"
        private const val REQUEST_LINK_DELAY = 750L

        // poll verification state every minute for 5 times
        private const val POLLING_INTERVAL_MS = 60L * 1000L
        private const val POLLING_COUNT = 5
    }
}
