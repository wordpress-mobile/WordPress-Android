package org.wordpress.android.fluxc.store

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.JetpackAction
import org.wordpress.android.fluxc.action.JetpackAction.ACTIVATE_STATS_MODULE
import org.wordpress.android.fluxc.action.JetpackAction.INSTALL_JETPACK
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackUser
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModuleErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val RELOAD_SITE_DELAY = 5000L

@Singleton
class JetpackStore
@Inject constructor(
    private val jetpackRestClient: JetpackRestClient,
    private val jetpackWPAPIRestClient: JetpackWPAPIRestClient,
    private val siteStore: SiteStore,
    private val coroutineEngine: CoroutineEngine,
    private val wpapiAuthenticator: WPAPIAuthenticator,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    private var siteContinuation: Continuation<Unit>? = null

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? JetpackAction ?: return
        when (actionType) {
            INSTALL_JETPACK -> {
                coroutineEngine.launch(T.SETTINGS, this, "JetpackAction.INSTALL_JETPACK") {
                    install(
                            action.payload as SiteModel,
                            actionType
                    )
                }
            }
            ACTIVATE_STATS_MODULE -> {
                coroutineEngine.launch(T.SETTINGS, this, "JetpackAction.ACTIVATE_STATS_MODULE") {
                    emitChange(activateStatsModule(action.payload as ActivateStatsModulePayload))
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, "JetpackStore onRegister")
    }

    suspend fun install(
        site: SiteModel,
        action: JetpackAction = INSTALL_JETPACK
    ) = coroutineEngine.withDefaultContext(T.SETTINGS, this, "install") {
        val installedPayload = jetpackRestClient.installJetpack(site)
        reloadSite(site)
        val reloadedSite = siteStore.getSiteByLocalId(site.id)
        val isJetpackInstalled = reloadedSite?.isJetpackInstalled == true
        return@withDefaultContext if (!installedPayload.isError || isJetpackInstalled) {
            val onJetpackInstall = OnJetpackInstalled(
                    installedPayload.success ||
                            isJetpackInstalled, action
            )
            emitChange(onJetpackInstall)
            onJetpackInstall
        } else {
            val errorPayload = OnJetpackInstalled(installedPayload.error, action)
            emitChange(errorPayload)
            errorPayload
        }
    }

    private suspend fun reloadSite(site: SiteModel) = suspendCancellableCoroutine<Unit> { cont ->
        siteStore.onAction(SiteActionBuilder.newFetchSiteAction(site))
        siteContinuation = cont
        val job = coroutineEngine.launch(T.SETTINGS, this, "reloadSite") {
            delay(RELOAD_SITE_DELAY)
            if (siteContinuation != null && siteContinuation == cont) {
                siteContinuation?.resume(Unit)
                siteContinuation = null
            }
        }
        cont.invokeOnCancellation {
            siteContinuation = null
            job.cancel()
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
        val causeOfChange: JetpackAction
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

    class JetpackInstallError(
        val type: JetpackInstallErrorType,
        val apiError: String? = null,
        val message: String? = null
    ) : OnChangedError

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.rowsAffected > 0) {
            siteContinuation?.resume(Unit)
            siteContinuation = null
        }
    }

    // Activate Jetpack Stats Module
    suspend fun activateStatsModule(requestPayload: ActivateStatsModulePayload): OnActivateStatsModule {
        val payload = jetpackRestClient.activateStatsModule(requestPayload)
        if (payload.success) {
            reloadSite(requestPayload.site)
            val reloadedSite = siteStore.getSiteByLocalId(requestPayload.site.id)
            val isStatsModuleActive = reloadedSite?.activeModules?.contains("stats") ?: false
            return emitActivateStatsModuleResult(payload, isStatsModuleActive)
        }
        return emitActivateStatsModuleResult(payload, false)
    }

    private fun emitActivateStatsModuleResult(
        payload: ActivateStatsModuleResultPayload,
        isStatsModuleActive: Boolean
    ): OnActivateStatsModule {
        return if (!payload.isError && isStatsModuleActive) {
            OnActivateStatsModule(ACTIVATE_STATS_MODULE)
        } else {
            OnActivateStatsModule(
                    ActivateStatsModuleError(GENERIC_ERROR, "Unable to activate stats"),
                    ACTIVATE_STATS_MODULE
            )
        }
    }

    class ActivateStatsModulePayload(val site: SiteModel) : Payload<BaseNetworkError>()

    data class ActivateStatsModuleResultPayload(
        val success: Boolean,
        val site: SiteModel
    ) : Payload<ActivateStatsModuleError>() {
        constructor(error: ActivateStatsModuleError, site: SiteModel) : this(success = false, site = site) {
            this.error = error
        }
    }

    enum class ActivateStatsModuleErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR
    }

    class ActivateStatsModuleError(
        val type: ActivateStatsModuleErrorType,
        val message: String? = null
    ) : OnChangedError

    suspend fun fetchJetpackConnectionUrl(site: SiteModel): JetpackConnectionUrlResult {
        if (site.isUsingWpComRestApi) error("This function supports only self-hosted site using WPAPI")
        return coroutineEngine.withDefaultContext(T.API, this, "fetchJetpackConnectionUrl") {
            val result = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
                jetpackWPAPIRestClient.fetchJetpackConnectionUrl(site, nonce)
            }

            when {
                result.isError -> JetpackConnectionUrlResult(JetpackConnectionUrlError(result.error?.message))
                result.result.isNullOrEmpty() -> JetpackConnectionUrlResult(
                    JetpackConnectionUrlError("Response Empty")
                )
                else -> {
                    val url = result.result.trim('"').replace("\\", "")
                    JetpackConnectionUrlResult(url)
                }
            }
        }
    }

    data class JetpackConnectionUrlResult(
        val url: String
    ) : Payload<JetpackConnectionUrlError>() {
        constructor(error: JetpackConnectionUrlError) : this("") {
            this.error = error
        }
    }

    class JetpackConnectionUrlError(
        val message: String? = null
    ) : OnChangedError

    suspend fun fetchJetpackUser(site: SiteModel): JetpackUserResult {
        if (site.isUsingWpComRestApi) error("This function is not implemented yet for Jetpack tunnel")
        return coroutineEngine.withDefaultContext(T.API, this, "fetchJetpackUser") {
            val result = wpapiAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
                jetpackWPAPIRestClient.fetchJetpackUser(site, nonce)
            }

            when {
                result.isError -> JetpackUserResult(JetpackUserError(result.error?.message))
                result.result == null -> JetpackUserResult(
                    JetpackUserError("Response Empty")
                )
                else -> {
                    JetpackUserResult(result.result)
                }
            }
        }
    }

    data class JetpackUserResult(
        val user: JetpackUser?
    ) : Payload<JetpackUserError>() {
        constructor(error: JetpackUserError) : this(null) {
            this.error = error
        }
    }

    class JetpackUserError(
        val message: String? = null
    ) : OnChangedError

    // Actions
    data class OnActivateStatsModule(
        val causeOfChange: JetpackAction
    ) : Store.OnChanged<ActivateStatsModuleError>() {
        constructor(
            error: ActivateStatsModuleError,
            causeOfChange: JetpackAction
        ) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }
}
