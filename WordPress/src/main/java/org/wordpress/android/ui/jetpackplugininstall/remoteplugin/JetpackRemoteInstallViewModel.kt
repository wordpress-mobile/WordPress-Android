package org.wordpress.android.ui.jetpackplugininstall.remoteplugin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_RESTART
import org.wordpress.android.analytics.AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_START
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.JetpackActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.JetpackConnectionSource
import org.wordpress.android.ui.JetpackConnectionUtils
import org.wordpress.android.ui.jetpackplugininstall.install.UiState
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONNECT
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONTACT_SUPPORT
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.LOGIN
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.MANUAL_INSTALL
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.Type.ERROR
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.Type.INSTALLED
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.Type.INSTALLING
import org.wordpress.android.ui.jetpackplugininstall.remoteplugin.JetpackRemoteInstallViewModel.Type.START
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

private const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
private const val FORBIDDEN = "FORBIDDEN"
private const val INSTALL_FAILURE = "INSTALL_FAILURE"
private const val INSTALL_RESPONSE_ERROR = "INSTALL_RESPONSE_ERROR"
private const val LOGIN_FAILURE = "LOGIN_FAILURE"
private const val SITE_IS_JETPACK = "SITE_IS_JETPACK"
private const val ACTIVATION_ON_INSTALL_FAILURE = "ACTIVATION_ON_INSTALL_FAILURE"
private const val ACTIVATION_RESPONSE_ERROR = "ACTIVATION_RESPONSE_ERROR"
private const val ACTIVATION_FAILURE = "ACTIVATION_FAILURE"
private val BLOCKING_FAILURES = listOf(
    FORBIDDEN,
    INSTALL_FAILURE,
    INSTALL_RESPONSE_ERROR,
    LOGIN_FAILURE,
    INVALID_CREDENTIALS,
    ACTIVATION_ON_INSTALL_FAILURE,
    ACTIVATION_RESPONSE_ERROR,
    ACTIVATION_FAILURE
)
private const val CONTEXT = "JetpackRemoteInstall"
private const val EMPTY_TYPE = "EMPTY_TYPE"
private const val EMPTY_MESSAGE = "EMPTY_MESSAGE"

@HiltViewModel
class JetpackRemoteInstallViewModel
@Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    // JetpackStore needs to be injected here as otherwise FluxC doesn't accept emitted events.
    @Suppress("unused") private val jetpackStore: JetpackStore
) : ViewModel() {
    private val mutableViewState = MutableLiveData<UiState>()
    val liveViewState: LiveData<UiState> = mutableViewState
    private val mutableActionOnResult = SingleLiveEvent<JetpackResultActionData>()
    val liveActionOnResult: LiveData<JetpackResultActionData> = mutableActionOnResult
    private lateinit var siteModel: SiteModel

    init {
        dispatcher.register(this)
    }

    fun initialize(site: SiteModel, type: Type?) {
        siteModel = site
        // Init state only if it's empty
        if (mutableViewState.value == null) {
            mutableViewState.value = type.toState()
            if (type == INSTALLING) startRemoteInstall(site)
        }
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun onLogin(siteId: Int) {
        onDoneButtonClick(siteId)
    }

    private fun Type?.toState(): UiState {
        return when (this) {
            null, START -> UiState.Initial(R.string.jetpack_plugin_install_initial_button)
            INSTALLING -> UiState.Installing
            INSTALLED -> UiState.Done(
                R.string.jetpack_plugin_install_remote_plugin_done_description,
                R.string.jetpack_plugin_install_remote_plugin_done_button
            )

            ERROR -> UiState.Error(
                R.string.jetpack_plugin_install_error_button_retry,
                R.string.jetpack_plugin_install_error_button_contact_support
            )
        }
    }

    fun onInitialButtonClick() {
        AnalyticsTracker.track(INSTALL_JETPACK_REMOTE_START)
        startRemoteInstall(siteModel)
    }

    fun onRetryButtonClick() {
        AnalyticsTracker.track(INSTALL_JETPACK_REMOTE_RESTART)
        startRemoteInstall(siteModel)
    }

    fun onDoneButtonClick(siteId: Int = siteModel.id) {
        val hasAccessToken = accountStore.hasAccessToken()
        val action = if (hasAccessToken) {
            AnalyticsTracker.track(Stat.INSTALL_JETPACK_REMOTE_CONNECT)
            CONNECT
        } else {
            AnalyticsTracker.track(Stat.INSTALL_JETPACK_REMOTE_LOGIN)
            LOGIN
        }
        triggerResultAction(siteId, action, hasAccessToken)
    }

    fun onContactSupportButtonClick() {
        mutableActionOnResult.postValue(
            JetpackResultActionData(
                siteModel,
                accountStore.hasAccessToken(),
                CONTACT_SUPPORT
            )
        )
    }

    fun isBackButtonEnabled() = mutableViewState.value?.showCloseButton != false

    fun onBackPressed(source: JetpackConnectionSource) {
        JetpackConnectionUtils.trackWithSource(
            Stat.INSTALL_JETPACK_CANCELLED,
            source
        )
    }

    private fun startRemoteInstall(site: SiteModel) {
        mutableViewState.postValue(INSTALLING.toState())
        dispatcher.dispatch(JetpackActionBuilder.newInstallJetpackAction(site))
    }

    private fun triggerResultAction(
        siteId: Int,
        action: Action,
        hasAccessToken: Boolean = accountStore.hasAccessToken()
    ) {
        mutableActionOnResult.postValue(
            JetpackResultActionData(
                siteStore.getSiteByLocalId(siteId)!!,
                hasAccessToken,
                action
            )
        )
    }

    // Network Callbacks
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEventsUpdated(event: OnJetpackInstalled) {
        val site = siteModel
        if (event.isError) {
            AnalyticsTracker.track(
                Stat.INSTALL_JETPACK_REMOTE_FAILED,
                CONTEXT,
                event.error?.apiError ?: EMPTY_TYPE,
                event.error?.message ?: EMPTY_MESSAGE
            )
            when {
                event.error?.apiError == SITE_IS_JETPACK -> {
                    AnalyticsTracker.track(Stat.INSTALL_JETPACK_REMOTE_COMPLETED)
                    mutableViewState.postValue(INSTALLED.toState())
                }

                BLOCKING_FAILURES.contains(event.error?.apiError) -> {
                    AnalyticsTracker.track(Stat.INSTALL_JETPACK_REMOTE_START_MANUAL_FLOW)
                    triggerResultAction(site.id, MANUAL_INSTALL)
                }

                else -> mutableViewState.postValue(ERROR.toState())
            }
            return
        }
        if (event.success) {
            AnalyticsTracker.track(Stat.INSTALL_JETPACK_REMOTE_COMPLETED)
            mutableViewState.postValue(INSTALLED.toState())
        } else {
            AnalyticsTracker.track(Stat.INSTALL_JETPACK_REMOTE_FAILED)
            mutableViewState.postValue(ERROR.toState())
        }
    }

    data class JetpackResultActionData(val site: SiteModel, val loggedIn: Boolean, val action: Action) {
        enum class Action {
            LOGIN, MANUAL_INSTALL, CONNECT, CONTACT_SUPPORT
        }
    }

    enum class Type {
        START, INSTALLING, INSTALLED, ERROR;

        companion object {
            fun fromState(state: UiState): Type {
                return when (state) {
                    is UiState.Initial -> START
                    is UiState.Installing -> INSTALLING
                    is UiState.Done -> INSTALLED
                    is UiState.Error -> ERROR
                }
            }
        }
    }
}

