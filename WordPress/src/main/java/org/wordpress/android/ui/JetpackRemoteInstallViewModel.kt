package org.wordpress.android.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Error
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Installed
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Start
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Type
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Type.ERROR
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Type.INSTALLED
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Type.INSTALLING
import org.wordpress.android.ui.JetpackRemoteInstallViewModel.ViewState.Type.START
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class JetpackRemoteInstallViewModel
@Inject constructor(
    private val jetpackStore: JetpackStore,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) : ViewModel() {
    private val mutableViewState = MutableLiveData<ViewState>()
    val liveViewState: LiveData<ViewState> = mutableViewState
    private val mutableJetpackConnectionFlow = SingleLiveEvent<JetpackConnectionData>()
    val liveJetpackConnectionFlow: LiveData<JetpackConnectionData> = mutableJetpackConnectionFlow
    private var job: Job? = null

    fun start(site: SiteModel, type: Type?, coroutineContext: CoroutineContext = CommonPool) {
        // Init state only if it's empty
        if (mutableViewState.value == null) {
            mutableViewState.value = type.toState(site, coroutineContext)
        }
    }

    fun onLogin(siteId: Int) {
        connectJetpack(siteId)
    }

    private fun Type?.toState(site: SiteModel, coroutineContext: CoroutineContext): ViewState {
        if (this == null) {
            return Start { startRemoteInstall(site, coroutineContext) }
        }
        return when (this) {
            START -> Start { startRemoteInstall(site, coroutineContext) }
            INSTALLING -> {
                startRemoteInstall(site, coroutineContext)
                ViewState.Installing
            }
            INSTALLED -> Installed { connectJetpack(site.id) }
            ERROR -> Error { startRemoteInstall(site, coroutineContext) }
        }
    }

    private fun startRemoteInstall(site: SiteModel, coroutineContext: CoroutineContext) {
        cancelJob()
        job = launch(coroutineContext) {
            mutableViewState.postValue(ViewState.Installing)
            val installResult = jetpackStore.install(site)
            if (isActive) {
                if (installResult.success) {
                    mutableViewState.postValue(Installed { connectJetpack(site.id) })
                } else {
                    mutableViewState.postValue(Error { startRemoteInstall(site, coroutineContext) })
                }
            }
        }
    }

    private fun connectJetpack(siteId: Int) {
        val site = siteStore.getSiteByLocalId(siteId)
        val hasAccessToken = accountStore.hasAccessToken()
        mutableJetpackConnectionFlow.postValue(JetpackConnectionData(site, hasAccessToken))
    }

    override fun onCleared() {
        super.onCleared()
        cancelJob()
    }

    private fun cancelJob() {
        job?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }

    data class JetpackConnectionData(val site: SiteModel, val loggedIn: Boolean)

    sealed class ViewState(
        val type: Type,
        @StringRes val titleResource: Int,
        @StringRes val messageResource: Int,
        @DrawableRes val icon: Int,
        @StringRes val buttonResource: Int? = null,
        open val onClick: () -> Unit = {},
        val progressBarVisible: Boolean = false
    ) {
        data class Start(override val onClick: () -> Unit) : ViewState(
                START,
                R.string.install_jetpack,
                R.string.install_jetpack_message,
                icon = R.drawable.ic_jetpack_icon_green_88dp,
                buttonResource = R.string.install_jetpack_continue,
                onClick = onClick
        )

        object Installing : ViewState(
                INSTALLING,
                R.string.installing_jetpack,
                R.string.installing_jetpack_message,
                icon = R.drawable.ic_jetpack_icon_green_88dp,
                progressBarVisible = true
        )

        data class Installed(override val onClick: () -> Unit) : ViewState(
                INSTALLED,
                R.string.jetpack_installed,
                R.string.jetpack_installed_message,
                icon = R.drawable.ic_jetpack_icon_green_88dp,
                buttonResource = R.string.install_jetpack_continue,
                onClick = onClick
        )

        data class Error(override val onClick: () -> Unit) : ViewState(
                ERROR,
                R.string.jetpack_installation_problem,
                R.string.jetpack_installation_problem_message,
                icon = R.drawable.ic_exclamation_mark_88dp,
                buttonResource = R.string.install_jetpack_retry,
                onClick = onClick
        )

        enum class Type {
            START, INSTALLING, INSTALLED, ERROR
        }
    }
}
