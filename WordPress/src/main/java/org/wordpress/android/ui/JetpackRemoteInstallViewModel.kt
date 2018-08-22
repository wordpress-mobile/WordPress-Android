package org.wordpress.android.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.JetpackActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.OnJetpackInstalled
import org.wordpress.android.fluxc.store.SiteStore
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

class JetpackRemoteInstallViewModel
@Inject constructor(
    private val jetpackStore: JetpackStore,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : ViewModel() {
    private val mutableViewState = MutableLiveData<JetpackRemoteInstallViewState>()
    val liveViewState: LiveData<JetpackRemoteInstallViewState> = mutableViewState
    private val mutableJetpackConnectionFlow = SingleLiveEvent<JetpackConnectionData>()
    val liveJetpackConnectionFlow: LiveData<JetpackConnectionData> = mutableJetpackConnectionFlow
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
        connectJetpack(siteId)
    }

    private fun Type?.toState(site: SiteModel): JetpackRemoteInstallViewState {
        if (this == null) {
            return Start { startRemoteInstall(site) }
        }
        return when (this) {
            START -> Start { startRemoteInstall(site) }
            INSTALLING -> {
                startRemoteInstall(site)
                JetpackRemoteInstallViewState.Installing
            }
            INSTALLED -> Installed { connectJetpack(site.id) }
            ERROR -> Error { startRemoteInstall(site) }
        }
    }

    private fun startRemoteInstall(site: SiteModel) {
        mutableViewState.postValue(JetpackRemoteInstallViewState.Installing)
        dispatcher.dispatch(JetpackActionBuilder.newInstallJetpackAction(site))
    }

    private fun connectJetpack(siteId: Int) {
        val site = siteStore.getSiteByLocalId(siteId)
        val hasAccessToken = accountStore.hasAccessToken()
        mutableJetpackConnectionFlow.postValue(JetpackConnectionData(site, hasAccessToken))
    }

    // Network Callbacks
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onEventsUpdated(event: OnJetpackInstalled) {
        val site = siteModel ?: return
        if (event.isError) {
            mutableViewState.postValue(Error { startRemoteInstall(site) })
            return
        }
        if (event.success) {
            mutableViewState.postValue(Installed { connectJetpack(site.id) })
        } else {
            mutableViewState.postValue(Error { startRemoteInstall(site) })
        }
    }

    data class JetpackConnectionData(val site: SiteModel, val loggedIn: Boolean)
}
