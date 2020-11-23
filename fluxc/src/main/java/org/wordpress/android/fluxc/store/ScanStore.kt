package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ScanAction
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_STATE
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanStore @Inject constructor(
    private val scanRestClient: ScanRestClient,
    private val scanSqlUtils: ScanSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ScanAction ?: return
        when (actionType) {
            FETCH_SCAN_STATE -> {
                coroutineEngine.launch(AppLog.T.API, this, "Scan: On FETCH_SCAN_STATE") {
                    emitChange(fetchScanState(action.payload as FetchScanStatePayload))
                }
            }
        }
    }

    fun getScanStateForSite(site: SiteModel): ScanStateModel? {
        return scanSqlUtils.getScanStateForSite(site)
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    suspend fun fetchScanState(fetchScanStatePayload: FetchScanStatePayload): OnScanStateFetched {
        val payload = scanRestClient.fetchScanState(fetchScanStatePayload.site)
        return storeScanState(payload)
    }

    private fun storeScanState(payload: FetchedScanStatePayload): OnScanStateFetched {
        return if (payload.error != null) {
            OnScanStateFetched(payload.error, FETCH_SCAN_STATE)
        } else {
            if (payload.scanStateModel != null) {
                scanSqlUtils.replaceScanState(payload.site, payload.scanStateModel)
            }
            OnScanStateFetched(FETCH_SCAN_STATE)
        }
    }

    // Actions
    data class OnScanStateFetched(var causeOfChange: ScanAction) : Store.OnChanged<ScanStateError>() {
        constructor(error: ScanStateError, causeOfChange: ScanAction) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    // Payloads
    class FetchScanStatePayload(
        val site: SiteModel
    ) : Payload<BaseNetworkError>()

    class FetchedScanStatePayload(
        val scanStateModel: ScanStateModel? = null,
        val site: SiteModel
    ) : Payload<ScanStateError>() {
        constructor(
            error: ScanStateError,
            site: SiteModel
        ) : this(site = site) {
            this.error = error
        }
    }

    // Errors
    enum class ScanStateErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class ScanStateError(var type: ScanStateErrorType, var message: String? = null) : OnChangedError
}
