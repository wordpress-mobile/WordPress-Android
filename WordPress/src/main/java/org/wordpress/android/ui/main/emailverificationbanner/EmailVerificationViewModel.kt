package org.wordpress.android.ui.main.emailverificationbanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
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
) : ScopedViewModel(mainDispatcher) {
    private val _emailVerificationState = MutableLiveData<EmailVerificationState>()
    val emailVerificationState: LiveData<EmailVerificationState> = _emailVerificationState

    var emailVerificationError: String = ""
        private set

    enum class EmailVerificationState {
        NO_ACCOUNT,     // user doesn't have an account so verification is not possible
        UNVERIFIED,     // user has not verified their email
        LINK_REQUESTED, // user has requested a verification link (API call in progress)
        LINK_SENT,      // verification link has been sent successfully (API call completed)
        LINK_ERROR,     // an error occurred requesting the verification link
        VERIFIED,       // user has verified their email address
    }

    init {
        dispatcher.register(this)
        _emailVerificationState.value = if (accountStore.account.emailVerified) {
            EmailVerificationState.VERIFIED
        } else if (accountStore.account.email.isNotEmpty()) {
            EmailVerificationState.UNVERIFIED
        } else {
            EmailVerificationState.NO_ACCOUNT
        }
    }

    private fun checkVerificationState(): Boolean {
        if (accountStore.account.emailVerified) {
            _emailVerificationState.value = EmailVerificationState.VERIFIED
            return true
        } else {
            return false
        }
    }

    /**
     * User clicked the "Send verification link" button on the email verification banner
     */
    fun onSendVerificationLinkClick() {
        if (!NetworkUtils.checkConnection(WordPress.getContext())) {
            return
        }
        _emailVerificationState.value = EmailVerificationState.LINK_REQUESTED
        // briefly delay the request so the user can see the updated banner if the request completes quickly
        launch {
            withContext(bgDispatcher) {
                delay(REQUEST_VERIFICATION_LINK_DELAY)
                dispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction())
            }
        }
    }

    /**
     * Repeatedly fetches the user's account to detect when the user has verified their email address
     */
    private fun pollVerificationState() {
        launch {
            for (i in 0..POLLING_COUNT) {
                dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
                delay(POLLING_INTERVAL) // TODO move this above dispatching
                withContext(mainDispatcher) {
                    if (checkVerificationState()) {
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
                emailVerificationError = event.error.message
                _emailVerificationState.value = EmailVerificationState.LINK_ERROR
                pollVerificationState() // TODO remove
            }
        } else if (event.causeOfChange == AccountAction.SENT_VERIFICATION_EMAIL) {
            _emailVerificationState.value = EmailVerificationState.LINK_SENT
            pollVerificationState()
        }
    }

    companion object {
        private const val REQUEST_VERIFICATION_LINK_DELAY = 750L
        private const val POLLING_INTERVAL = 60L * 1000L    // poll verification state every minute
        private const val POLLING_COUNT = 5                 // poll verification state 5 times
    }
}
