package org.wordpress.android.ui.main

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.models.recommend.RecommendApiCallsProvider
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendAppName
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Success
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.recommend.RecommendAppState
import org.wordpress.android.ui.recommend.RecommendAppState.ApiFetchedResult
import org.wordpress.android.ui.recommend.RecommendAppState.FetchingApi
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.RecommendAppSource.ME
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MeViewModel
@Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val recommendApiCallsProvider: RecommendApiCallsProvider,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
) : ScopedViewModel(mainDispatcher) {
    private val _showDisconnectDialog = MutableLiveData<Event<Boolean>>()
    val showDisconnectDialog: LiveData<Event<Boolean>> = _showDisconnectDialog

    private val _recommendUiState = MutableLiveData<RecommendAppState>()
    val recommendUiState: LiveData<Event<RecommendAppUiState>> = _recommendUiState.map { it.toUiState() }

    private val _showUnifiedAbout = MutableLiveData<Event<Boolean>>()
    val showUnifiedAbout: LiveData<Event<Boolean>> = _showUnifiedAbout

    private val _showScanLoginCode = MutableLiveData<Event<Boolean>>()
    val showScanLoginCode: LiveData<Event<Boolean>> = _showScanLoginCode

    private val _showJetpackPoweredBottomSheet = MutableLiveData<Event<Boolean>>()
    val showJetpackPoweredBottomSheet: LiveData<Event<Boolean>> = _showJetpackPoweredBottomSheet

    private val _emailVerificationState = MutableLiveData<EmailVerificationState>()
    val emailVerificationState: LiveData<EmailVerificationState> = _emailVerificationState

    private val _errorMessage = MutableLiveData<UiString>()
    val errorMessage: LiveData<UiString> = _errorMessage

    private var isEmailVerificationLinkRequested: Boolean = false
    private var isEmailVerificationLinkSent: Boolean = false

    data class RecommendAppUiState(
        val showLoading: Boolean = false,
        val error: String? = null,
        val message: String,
        val link: String
    ) {
        constructor(showLoading: Boolean) : this(
            showLoading = showLoading,
            message = "",
            link = ""
        )

        constructor(error: String) : this(
            error = error,
            message = "",
            link = ""
        )

        fun isError() = error != null
    }

    enum class EmailVerificationState {
        NO_ACCOUNT,     // user doesn't have an account so verification is not possible
        UNVERIFIED,     // user has not verified their email
        LINK_REQUESTED, // user has requested a verification link
        LINK_SENT,      // verification link has been sent
        VERIFIED,       // user has verified their email address
    }

    init {
        dispatcher.register(this)
        updateEmailVerificationState()
    }

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

    fun showScanLoginCode() {
        _showScanLoginCode.value = Event(true)
    }

    fun showJetpackPoweredBottomSheet() {
        _showJetpackPoweredBottomSheet.value = Event(true)
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun onRecommendTheApp() {
        when (val state = _recommendUiState.value) {
            is ApiFetchedResult -> {
                if (state.isError()) {
                    getRecommendTemplate()
                } else {
                    _recommendUiState.value = state
                }
            }

            FetchingApi -> {
                return
            }

            null -> {
                getRecommendTemplate()
            }
        }
    }

    private fun updateEmailVerificationState() {
        val hasEmail = accountStore.account.email.isNotEmpty()
        val isEmailVerified = hasEmail && accountStore.account.emailVerified
        _emailVerificationState.value = if (!isEmailVerified) { // TODO remove !
            EmailVerificationState.VERIFIED
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

    fun sendVerificationLinkClick(context: Context) {
        if (!NetworkUtils.checkConnection(context)) {
            return
        }
        if (accountStore.hasAccessToken()) {
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            if (event.error.type == AccountErrorType.SEND_VERIFICATION_EMAIL_ERROR) {
                isEmailVerificationLinkRequested = false
                isEmailVerificationLinkSent = false
                if (event.error.message.isNotEmpty()) {
                    _errorMessage.value = UiString.UiStringText(event.error.message)
                } else {
                    _errorMessage.value = UiString.UiStringRes(R.string.me_email_verification_generic_error)
                }
            }
        } else if (event.causeOfChange == AccountAction.SENT_VERIFICATION_EMAIL) {
            isEmailVerificationLinkSent = true
        }

        updateEmailVerificationState()
    }

    private fun getRecommendTemplate() {
        launch {
            _recommendUiState.value = FetchingApi
            withContext(bgDispatcher) {
                delay(SHOW_LOADING_DELAY)

                val fetchedResult = recommendApiCallsProvider.getRecommendTemplate(
                    if (BuildConfig.IS_JETPACK_APP) {
                        RecommendAppName.Jetpack.appName
                    } else {
                        RecommendAppName.WordPress.appName
                    },
                    ME
                ).toFetchedResult()

                _recommendUiState.postValue(fetchedResult)
            }
        }
    }

    private fun RecommendCallResult.toFetchedResult(): ApiFetchedResult {
        return when (this) {
            is Failure -> ApiFetchedResult(error = this.error)
            is Success -> ApiFetchedResult(
                message = this.templateData.message,
                link = this.templateData.link
            )
        }
    }

    private fun RecommendAppState.toUiState(): Event<RecommendAppUiState> {
        return Event(
            when (this) {
            is ApiFetchedResult -> if (this.isError()) {
                RecommendAppUiState(this.error!!)
            } else {
                RecommendAppUiState(
                    link = this.link,
                    message = this.message
                ).also {
                    analyticsUtilsWrapper.trackRecommendAppEngaged(ME)
                }
            }

            FetchingApi -> RecommendAppUiState(showLoading = true)
        })
    }

    companion object {
        private const val SHOW_LOADING_DELAY = 300L
        private const val REQUEST_VERIFICATION_LINK_DELAY = 500L
    }
}
