package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.JetpackAction
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackStore
@Inject constructor(private val jetpackRestClient: JetpackRestClient, dispatcher: Dispatcher) : Store(dispatcher) {
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

    suspend fun install(site: SiteModel, action: JetpackAction = INSTALL_JETPACK): OnJetpackInstalled {
        val installedPayload = jetpackRestClient.installJetpack(site)
        return if (!installedPayload.isError) {
            val onJetpackInstall = OnJetpackInstalled(installedPayload.success, action)
            emitChange(onJetpackInstall)
            onJetpackInstall
        } else {
            val errorPayload = OnJetpackInstalled(installedPayload.error, action)
            emitChange(errorPayload)
            errorPayload
        }
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
}
