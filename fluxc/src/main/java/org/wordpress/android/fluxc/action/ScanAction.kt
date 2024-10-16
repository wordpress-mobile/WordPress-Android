package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.ScanStore

@ActionEnum
enum class ScanAction : IAction {
    @Action(payloadType = ScanStore.FetchScanStatePayload::class)
    FETCH_SCAN_STATE,
    @Action(payloadType = ScanStore.ScanStartPayload::class)
    START_SCAN,
    @Action(payloadType = ScanStore.FixThreatsPayload::class)
    FIX_THREATS,
    @Action(payloadType = ScanStore.IgnoreThreatPayload::class)
    IGNORE_THREAT,
    @Action(payloadType = ScanStore.FetchFixThreatsStatusPayload::class)
    FETCH_FIX_THREATS_STATUS,
    @Action(payloadType = ScanStore.FetchScanHistoryPayload::class)
    FETCH_SCAN_HISTORY,
}
