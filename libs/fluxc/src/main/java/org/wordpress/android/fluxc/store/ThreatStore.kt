package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ThreatAction
import org.wordpress.android.fluxc.action.ThreatAction.FETCH_THREAT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.ThreatRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatStore @Inject constructor(
    private val threatRestClient: ThreatRestClient,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ThreatAction ?: return
        when (actionType) {
            FETCH_THREAT -> {
                coroutineEngine.launch(AppLog.T.API, this, "On FETCH_THREAT") {
                    emitChange(fetchThreat(action.payload as FetchThreatPayload))
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    suspend fun fetchThreat(fetchThreatPayload: FetchThreatPayload): OnThreatFetched {
        val payload = threatRestClient.fetchThreat(fetchThreatPayload.site, fetchThreatPayload.threatId)
        return if (payload.error != null) {
            OnThreatFetched(payload.error, FETCH_THREAT)
        } else {
            OnThreatFetched(payload.threatModel, FETCH_THREAT)
        }
    }

    // Actions
    data class OnThreatFetched(
        val threatModel: ThreatModel? = null,
        var causeOfChange: ThreatAction
    ) : Store.OnChanged<ThreatError>() {
        constructor(error: ThreatError, causeOfChange: ThreatAction) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    // Payloads
    class FetchThreatPayload(
        val site: SiteModel,
        val threatId: Long
    ) : Payload<BaseNetworkError>()

    class FetchedThreatPayload(
        val threatModel: ThreatModel? = null,
        val site: SiteModel
    ) : Payload<ThreatError>() {
        constructor(error: ThreatError, site: SiteModel) : this(site = site) {
            this.error = error
        }
    }

    // Errors
    enum class ThreatErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR,
        MISSING_THREAT_ID,
        MISSING_SIGNATURE,
        MISSING_FIRST_DETECTED
    }

    class ThreatError(var type: ThreatErrorType, var message: String? = null) : OnChangedError
}
