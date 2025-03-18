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
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
) : ScopedViewModel(mainDispatcher) {
    private val _emailVerificationState = MutableLiveData<EmailVerificationState>()
    val emailVerificationState: LiveData<EmailVerificationState> = _emailVerificationState

    private var isEmailVerificationLinkRequested: Boolean = false
    private var isEmailVerificationLinkSent: Boolean = false
    private var isEmailVerificationError: Boolean = false

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
        updateEmailVerificationState()
    }

    /*
     * Update the email verification state, called when we suspect the state has changed
     */
    private fun updateEmailVerificationState() {
        val hasEmail = accountStore.account.email.isNotEmpty()
        val isEmailVerified = hasEmail && accountStore.account.emailVerified
        _emailVerificationState.value = if (!isEmailVerified) { // TODO remove the !
            EmailVerificationState.VERIFIED
        } else if (isEmailVerificationError) {
            EmailVerificationState.LINK_ERROR
        } else if (isEmailVerificationLinkSent) {
            EmailVerificationState.LINK_SENT
        } else if (isEmailVerificationLinkRequested) {
            EmailVerificationState.LINK_REQUESTED
        } else if (hasEmail) {
            EmailVerificationState.UNVERIFIED
        } else {
            EmailVerificationState.NO_ACCOUNT
        }
    }

    /**
     * User clicked the "Send verification link" button on the email verification banner
     */
    fun onSendVerificationLinkClick() {
        if (!NetworkUtils.checkConnection(WordPress.getContext())) {
            return
        }
        if (accountStore.hasAccessToken()) {
            isEmailVerificationError = false
            isEmailVerificationLinkRequested = true
            updateEmailVerificationState()
            // briefly delay the request so the user can see the updated banner if the request completes quickly
            launch {
                withContext(bgDispatcher) {
                    delay(REQUEST_VERIFICATION_LINK_DELAY)
                    dispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction())
                }
            }
        }
    }

    /**
     * FluxC event for when the account state changes. Note that we only care about the email verification link
     * request since that's the only account event sent from this view model.
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            if (event.error.type == AccountErrorType.SEND_VERIFICATION_EMAIL_ERROR) {
                isEmailVerificationLinkRequested = false
                isEmailVerificationLinkSent = false
                // TODO we ignore event.error because it's blank
                isEmailVerificationError = true
            }
        } else if (event.causeOfChange == AccountAction.SENT_VERIFICATION_EMAIL) {
            isEmailVerificationLinkSent = true
            isEmailVerificationError = false
        }

        updateEmailVerificationState()
    }

    companion object {
        private const val REQUEST_VERIFICATION_LINK_DELAY = 750L
    }
}
