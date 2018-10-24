package org.wordpress.android.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_RESTART
import org.wordpress.android.analytics.AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_START
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.JetpackActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.CONNECT
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.LOGIN
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.JetpackResultActionData.Action.MANUAL_INSTALL
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Error
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Installed
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Start
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.ERROR
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.INSTALLED
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.INSTALLING
import org.wordpress.android.ui.JetpackRemoteInstallViewState.Type.START
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

class JetpackRemoteInstallViewModel
@Inject constructor(
    private val jetpackStore: JetpackStore,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : ViewModel() {
    private val mutableViewState = MutableLiveData<JetpackRemoteInstallViewState>()
    val liveViewState: LiveData<JetpackRemoteInstallViewState> = mutableViewState
    private val mutableActionOnResult = SingleLiveEvent<JetpackResultActionData>()
    val liveActionOnResult: LiveData<JetpackResultActionData> = mutableActionOnResult
    private var siteModel: SiteModel? = null

    init {
        dispatcher.register(this)
    }

    fun start(site: SiteModel, type: Type?) {
        siteModel = site
        // Init state only if it's empty
        if (mutableViewState.value == null) {
            mutableViewState.value = type.toState(site)
        }
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun onLogin(siteId: Int) {
        connect(siteId)
    }

    private fun Type?.toState(site: SiteModel): JetpackRemoteInstallViewState {
        if (this == null) {
            return Start { start(site) }
        }
        return when (this) {
            START -> Start { start(site) }
            INSTALLING -> {
                startRemoteInstall(site)
                JetpackRemoteInstallViewState.Installing
            }
            INSTALLED -> Installed { connect(site.id) }
            ERROR -> Error { restart(site) }
        }
    }

    private fun start(site: SiteModel) {
        AnalyticsTracker.track(INSTALL_JETPACK_REMOTE_START)
        startRemoteInstall(site)
    }

    private fun restart(site: SiteModel) {
        AnalyticsTracker.track(INSTALL_JETPACK_REMOTE_RESTART)
        startRemoteInstall(site)
    }

    private fun connect(siteId: Int) {
        val hasAccessToken = accountStore.hasAccessToken()
        val action = if (hasAccessToken) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_CONNECT)
            CONNECT
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_LOGIN)
            LOGIN
        }
        triggerResultAction(siteId, action, hasAccessToken)
    }

    private fun startRemoteInstall(site: SiteModel) {
        mutableViewState.postValue(JetpackRemoteInstallViewState.Installing)
        dispatcher.dispatch(JetpackActionBuilder.newInstallJetpackAction(site))
    }

    private fun triggerResultAction(
        siteId: Int,
        action: Action,
        hasAccessToken: Boolean = accountStore.hasAccessToken()
    ) {
        mutableActionOnResult.postValue(
                JetpackResultActionData(
                        siteStore.getSiteByLocalId(siteId),
                        hasAccessToken,
                        action
                )
        )
    }

    // Network Callbacks
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onEventsUpdated(event: OnJetpackInstalled) {
        val site = siteModel ?: return
        if (event.isError) {
            AnalyticsTracker.track(
                    AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_FAILED,
                    CONTEXT,
                    event.error?.apiError ?: EMPTY_TYPE,
                    event.error?.message ?: EMPTY_MESSAGE
            )
            when {
                event.error?.apiError == SITE_IS_JETPACK -> {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_COMPLETED)
                    mutableViewState.postValue(Installed { connect(site.id) })
                }
                BLOCKING_FAILURES.contains(event.error?.apiError) -> {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_START_MANUAL_FLOW)
                    triggerResultAction(site.id, MANUAL_INSTALL)
                }
                else -> mutableViewState.postValue(Error {
                    restart(site)
                })
            }
            return
        }
        if (event.success) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_COMPLETED)
            mutableViewState.postValue(Installed { connect(site.id) })
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.INSTALL_JETPACK_REMOTE_FAILED)
            mutableViewState.postValue(Error { restart(site) })
        }
    }

    data class JetpackResultActionData(val site: SiteModel, val loggedIn: Boolean, val action: Action) {
        enum class Action {
            LOGIN, MANUAL_INSTALL, CONNECT
        }
    }
}
