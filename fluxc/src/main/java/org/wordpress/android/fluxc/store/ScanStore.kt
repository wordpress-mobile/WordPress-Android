package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ScanAction
import org.wordpress.android.fluxc.action.ScanAction.FETCH_FIX_THREATS_STATUS
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_HISTORY
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_STATE
import org.wordpress.android.fluxc.action.ScanAction.FIX_THREATS
import org.wordpress.android.fluxc.action.ScanAction.IGNORE_THREAT
import org.wordpress.android.fluxc.action.ScanAction.START_SCAN
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.CURRENT
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.persistence.ThreatSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.BuildConfigWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

private val SCAN_THREAT_STATUSES = listOf(CURRENT)
private val SCAN_HISTORY_THREAT_STATUSES = listOf(IGNORED, FIXED)

@Singleton
class ScanStore @Inject constructor(
    private val scanRestClient: ScanRestClient,
    private val scanSqlUtils: ScanSqlUtils,
    private val threatSqlUtils: ThreatSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    private val appLogWrapper: AppLogWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
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
            START_SCAN -> {
                coroutineEngine.launch(AppLog.T.API, this, "Scan: On START_SCAN") {
                    emitChange(startScan(action.payload as ScanStartPayload))
                }
            }
            FIX_THREATS -> {
                coroutineEngine.launch(AppLog.T.API, this, "Scan: On FIX_THREATS") {
                    emitChange(fixThreats(action.payload as FixThreatsPayload))
                }
            }
            IGNORE_THREAT -> {
                coroutineEngine.launch(AppLog.T.API, this, "Scan: On IGNORE_THREAT") {
                    emitChange(ignoreThreat(action.payload as IgnoreThreatPayload))
                }
            }
            FETCH_FIX_THREATS_STATUS -> {
                coroutineEngine.launch(AppLog.T.API, this, "Scan: On FETCH_FIX_THREATS_STATUS") {
                    emitChange(fetchFixThreatsStatus(action.payload as FetchFixThreatsStatusPayload))
                }
            }
            FETCH_SCAN_HISTORY -> {
                coroutineEngine.launch(AppLog.T.API, this, "Scan: On FETCH_SCAN_HISTORY") {
                    emitChange(fetchScanHistory(action.payload as FetchScanHistoryPayload))
                }
            }
        }
    }

    suspend fun getScanStateForSite(site: SiteModel) =
        coroutineEngine.withDefaultContext(AppLog.T.JETPACK_SCAN, this, "getScanStateForSite") {
            val scanStateModel = scanSqlUtils.getScanStateForSite(site)
            val threats = scanStateModel?.let { threatSqlUtils.getThreats(site, SCAN_THREAT_STATUSES) }
            scanStateModel?.copy(threats = threats)
        }

    suspend fun getScanHistoryForSite(site: SiteModel) =
        coroutineEngine.withDefaultContext(AppLog.T.JETPACK_SCAN, this, "getScanHistoryForSite") {
            threatSqlUtils.getThreats(site, SCAN_HISTORY_THREAT_STATUSES)
        }

    suspend fun getThreatModelByThreatId(threatId: Long) =
        coroutineEngine.withDefaultContext(AppLog.T.JETPACK_SCAN, this, "getThreatModelByThreatId") {
            threatSqlUtils.getThreatByThreatId(threatId)
        }

    suspend fun hasValidCredentials(site: SiteModel) =
        coroutineEngine.withDefaultContext(AppLog.T.JETPACK_SCAN, this, "hasValidCredentials") {
            val scanStateModel = scanSqlUtils.getScanStateForSite(site)
            scanStateModel?.hasValidCredentials ?: false
        }

    suspend fun addOrUpdateScanStateModelForSite(action: ScanAction, site: SiteModel, scanStateModel: ScanStateModel) {
        coroutineEngine.withDefaultContext(AppLog.T.JETPACK_SCAN, this, "addOrUpdateScanStateModelForSite") {
            scanSqlUtils.replaceScanState(site, scanStateModel)
            storeThreatsWithStatuses(action, site, scanStateModel.threats, SCAN_THREAT_STATUSES)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, this.javaClass.name + ": onRegister")
    }

    suspend fun fetchScanState(fetchScanStatePayload: FetchScanStatePayload): OnScanStateFetched {
        val payload = scanRestClient.fetchScanState(fetchScanStatePayload.site)
        return storeScanState(payload)
    }

    private suspend fun storeScanState(payload: FetchedScanStatePayload): OnScanStateFetched {
        return if (payload.error != null) {
            OnScanStateFetched(payload.error, FETCH_SCAN_STATE)
        } else {
            payload.scanStateModel?.let {
                addOrUpdateScanStateModelForSite(FETCH_SCAN_STATE, payload.site, payload.scanStateModel)
            }
            OnScanStateFetched(FETCH_SCAN_STATE)
        }
    }

    suspend fun startScan(scanStartPayload: ScanStartPayload): OnScanStarted {
        val payload = scanRestClient.startScan(scanStartPayload.site)
        return emitScanStartResult(payload)
    }

    private fun emitScanStartResult(payload: ScanStartResultPayload): OnScanStarted {
        return if (payload.error != null) {
            OnScanStarted(payload.error, START_SCAN)
        } else {
            OnScanStarted(START_SCAN)
        }
    }

    suspend fun fixThreats(fixThreatsPayload: FixThreatsPayload): OnFixThreatsStarted {
        val payload = scanRestClient.fixThreats(fixThreatsPayload.remoteSiteId, fixThreatsPayload.threatIds)
        return emitFixThreatsResult(payload)
    }

    private fun emitFixThreatsResult(payload: FixThreatsResultPayload): OnFixThreatsStarted {
        return if (payload.error != null) {
            OnFixThreatsStarted(payload.error, FIX_THREATS)
        } else {
            OnFixThreatsStarted(FIX_THREATS)
        }
    }

    suspend fun ignoreThreat(ignoreThreatPayload: IgnoreThreatPayload): OnIgnoreThreatStarted {
        val payload = scanRestClient.ignoreThreat(ignoreThreatPayload.remoteSiteId, ignoreThreatPayload.threatId)
        return emitIgnoreThreatResult(payload)
    }

    private fun emitIgnoreThreatResult(payload: IgnoreThreatResultPayload): OnIgnoreThreatStarted {
        return if (payload.error != null) {
            OnIgnoreThreatStarted(payload.error, IGNORE_THREAT)
        } else {
            OnIgnoreThreatStarted(IGNORE_THREAT)
        }
    }

    suspend fun fetchFixThreatsStatus(payload: FetchFixThreatsStatusPayload): OnFixThreatsStatusFetched {
        val resultPayload = scanRestClient.fetchFixThreatsStatus(payload.remoteSiteId, payload.threatIds)
        return emitFixThreatsStatus(resultPayload)
    }

    private fun emitFixThreatsStatus(payload: FetchFixThreatsStatusResultPayload) = if (payload.error != null) {
        OnFixThreatsStatusFetched(payload.remoteSiteId, payload.error, FETCH_FIX_THREATS_STATUS)
    } else {
        OnFixThreatsStatusFetched(
            remoteSiteId = payload.remoteSiteId,
            fixThreatStatusModels = payload.fixThreatStatusModels,
            causeOfChange = FETCH_FIX_THREATS_STATUS
        )
    }

    suspend fun fetchScanHistory(payload: FetchScanHistoryPayload): OnScanHistoryFetched {
        val resultPayload = scanRestClient.fetchScanHistory(payload.site.siteId)
        if (!resultPayload.isError && resultPayload.threats != null) {
            storeThreatsWithStatuses(
                    FETCH_SCAN_HISTORY,
                    payload.site,
                    resultPayload.threats,
                    SCAN_HISTORY_THREAT_STATUSES
            )
        }
        return emitFetchScanHistoryResult(resultPayload)
    }

    @Suppress("TooGenericExceptionThrown")
    private fun storeThreatsWithStatuses(
        action: ScanAction,
        site: SiteModel,
        threats: List<ThreatModel>?,
        statuses: List<ThreatStatus>
    ) {
        threatSqlUtils.removeThreatsWithStatus(site, statuses)
        threats?.filter { statuses.contains(it.baseThreatModel.status) }
                ?.also {
                    if (it.size != threats.size) {
                        val msg = "$action action returned a Threat with ThreatState not in ${statuses.joinToString()}"
                        appLogWrapper.e(AppLog.T.API, msg)
                        if (buildConfigWrapper.isDebug()) throw RuntimeException(msg)
                    }
                }
                ?.run { threatSqlUtils.insertThreats(site, this) }
    }

    private fun emitFetchScanHistoryResult(payload: FetchScanHistoryResultPayload) =
            OnScanHistoryFetched(payload.remoteSiteId, payload.error, FETCH_SCAN_HISTORY)

    // Actions
    data class OnScanStateFetched(
        val causeOfChange: ScanAction
    ) : Store.OnChanged<ScanStateError>() {
        constructor(error: ScanStateError, causeOfChange: ScanAction) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnScanStarted(
        val causeOfChange: ScanAction
    ) : Store.OnChanged<ScanStartError>() {
        constructor(error: ScanStartError, causeOfChange: ScanAction) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnFixThreatsStarted(
        val causeOfChange: ScanAction
    ) : Store.OnChanged<FixThreatsError>() {
        constructor(error: FixThreatsError, causeOfChange: ScanAction) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnIgnoreThreatStarted(
        val causeOfChange: ScanAction
    ) : Store.OnChanged<IgnoreThreatError>() {
        constructor(error: IgnoreThreatError, causeOfChange: ScanAction) : this(causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnFixThreatsStatusFetched(
        val remoteSiteId: Long,
        val fixThreatStatusModels: List<FixThreatStatusModel>,
        val causeOfChange: ScanAction
    ) : Store.OnChanged<FixThreatsStatusError>() {
        constructor(
            remoteSiteId: Long,
            error: FixThreatsStatusError,
            causeOfChange: ScanAction
        ) : this(remoteSiteId = remoteSiteId, fixThreatStatusModels = emptyList(), causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    data class OnScanHistoryFetched(
        val remoteSiteId: Long,
        val causeOfChange: ScanAction
    ) : Store.OnChanged<FetchScanHistoryError>() {
        constructor(
            remoteSiteId: Long,
            error: FetchScanHistoryError?,
            causeOfChange: ScanAction
        ) : this(remoteSiteId = remoteSiteId, causeOfChange = causeOfChange) {
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

    class ScanStartPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class ScanStartResultPayload(val site: SiteModel) : Payload<ScanStartError>() {
        constructor(error: ScanStartError, site: SiteModel) : this(site = site) {
            this.error = error
        }
    }

    class FixThreatsPayload(val remoteSiteId: Long, val threatIds: List<Long>) : Payload<BaseNetworkError>()

    class FixThreatsResultPayload(val remoteSiteId: Long) : Payload<FixThreatsError>() {
        constructor(error: FixThreatsError, remoteSiteId: Long) : this(remoteSiteId = remoteSiteId) {
            this.error = error
        }
    }

    class IgnoreThreatPayload(val remoteSiteId: Long, val threatId: Long) : Payload<BaseNetworkError>()

    class IgnoreThreatResultPayload(
        val remoteSiteId: Long
    ) : Payload<IgnoreThreatError>() {
        constructor(error: IgnoreThreatError, remoteSiteId: Long) : this(remoteSiteId = remoteSiteId) {
            this.error = error
        }
    }

    class FetchFixThreatsStatusPayload(val remoteSiteId: Long, val threatIds: List<Long>) : Payload<BaseNetworkError>()

    class FetchFixThreatsStatusResultPayload(
        val remoteSiteId: Long,
        val fixThreatStatusModels: List<FixThreatStatusModel>
    ) : Payload<FixThreatsStatusError>() {
        constructor(
            remoteSiteId: Long,
            fixThreatStatusModels: List<FixThreatStatusModel> = emptyList(),
            error: FixThreatsStatusError
        ) : this(remoteSiteId = remoteSiteId, fixThreatStatusModels = fixThreatStatusModels) {
            this.error = error
        }
    }

    class FetchScanHistoryPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchScanHistoryResultPayload(
        val remoteSiteId: Long,
        val threats: List<ThreatModel>?
    ) : Payload<FetchScanHistoryError>() {
        constructor(
            remoteSiteId: Long,
            error: FetchScanHistoryError
        ) : this(remoteSiteId = remoteSiteId, threats = listOf()) {
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

    enum class ScanStartErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR
    }

    class ScanStartError(var type: ScanStartErrorType, var message: String? = null) : OnChangedError

    enum class FixThreatsErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR
    }

    class FixThreatsError(var type: FixThreatsErrorType, var message: String? = null) : OnChangedError

    enum class IgnoreThreatErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class IgnoreThreatError(var type: IgnoreThreatErrorType, var message: String? = null) : OnChangedError

    enum class FixThreatsStatusErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_THREAT_ID,
        API_ERROR
    }

    class FixThreatsStatusError(var type: FixThreatsStatusErrorType, var message: String? = null) : OnChangedError

    enum class FetchScanHistoryErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class FetchScanHistoryError(var type: FetchScanHistoryErrorType, var message: String? = null) : OnChangedError
}
