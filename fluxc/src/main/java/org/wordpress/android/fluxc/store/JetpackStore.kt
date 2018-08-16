package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.JetpackAction
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class JetpackStore
@Inject constructor(
    private val jetpackRestClient: JetpackRestClient,
    private val siteStore: SiteStore,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    private var siteContinuation: Continuation<Unit>? = null
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? JetpackAction ?: return
        when (actionType) {
            JetpackAction.INSTALL_JETPACK -> {
                launch { install(action.payload as SiteModel, actionType) }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, "JetpackStore onRegister")
    }

    suspend fun install(site: SiteModel, action: JetpackAction = INSTALL_JETPACK) = withContext(CommonPool) {
        val installedPayload = jetpackRestClient.installJetpack(site)
        reloadSite(site)
        val reloadedSite = siteStore.getSiteByLocalId(site.id)
        return@withContext if (!installedPayload.isError || reloadedSite.isJetpackInstalled) {
            val onJetpackInstall = OnJetpackInstalled(installedPayload.success ||
                    reloadedSite.isJetpackInstalled, action)
            emitChange(onJetpackInstall)
            onJetpackInstall
        } else {
            val errorPayload = OnJetpackInstalled(installedPayload.error, action)
            emitChange(errorPayload)
            errorPayload
        }
    }

    private suspend fun reloadSite(site: SiteModel) = suspendCoroutine<Unit> { cont ->
        launch {
            delay(5000)
            if (siteContinuation != null && siteContinuation == cont) {
                siteContinuation?.resume(Unit)
                siteContinuation = null
            }
        }
        siteStore.onAction(SiteActionBuilder.newFetchSiteAction(site))
        siteContinuation = cont
    }

    class JetpackInstalledPayload(
        val site: SiteModel,
        val success: Boolean
    ) : Payload<JetpackInstallError>() {
        constructor(
            error: JetpackInstallError,
            site: SiteModel
        ) : this(site = site, success = false) {
            this.error = error
        }
    }

    data class OnJetpackInstalled(
        val success: Boolean,
        var causeOfChange: JetpackAction
    ) : Store.OnChanged<JetpackInstallError>() {
        constructor(error: JetpackInstallError, causeOfChange: JetpackAction) :
                this(success = false, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    enum class JetpackInstallErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        USERNAME_OR_PASSWORD_MISSING,
        SITE_IS_JETPACK
    }

    class JetpackInstallError(var type: JetpackInstallErrorType, var message: String? = null) : Store.OnChangedError

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.rowsAffected > 0) {
            siteContinuation?.resume(Unit)
            siteContinuation = null
        }
    }
}
