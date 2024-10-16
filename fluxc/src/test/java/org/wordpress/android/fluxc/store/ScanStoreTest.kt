package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ScanAction
import org.wordpress.android.fluxc.generated.ScanActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Reason
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.model.scan.threat.BaseThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.GenericThreatModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.CURRENT
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.FIXED
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus.IGNORED
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.persistence.ThreatSqlUtils
import org.wordpress.android.fluxc.store.ScanStore.FetchFixThreatsStatusPayload
import org.wordpress.android.fluxc.store.ScanStore.FetchFixThreatsStatusResultPayload
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsError
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsErrorType
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsPayload
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsResultPayload
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusError
import org.wordpress.android.fluxc.store.ScanStore.FixThreatsStatusErrorType
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatError
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatErrorType
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatPayload
import org.wordpress.android.fluxc.store.ScanStore.IgnoreThreatResultPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartError
import org.wordpress.android.fluxc.store.ScanStore.ScanStartErrorType
import org.wordpress.android.fluxc.store.ScanStore.ScanStartPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartResultPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStateError
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.BuildConfigWrapper

@RunWith(MockitoJUnitRunner::class)
class ScanStoreTest {
    @Mock private lateinit var scanRestClient: ScanRestClient
    @Mock private lateinit var scanSqlUtils: ScanSqlUtils
    @Mock private lateinit var threatSqlUtils: ThreatSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var buildConfigWrapper: BuildConfigWrapper

    private val threatInCurrentState: ThreatModel = GenericThreatModel(
            BaseThreatModel(
                    1L,
                    "",
                    "",
                    CURRENT,
                    mock(),
                    mock(),
                    mock()
            )
    )
    private val threatInFixedState: ThreatModel = GenericThreatModel(
            threatInCurrentState.baseThreatModel.copy(status = FIXED)

    )

    private lateinit var scanStore: ScanStore
    private val siteId = 11L
    private val threatId = 1L
    private val threatIds = listOf(threatId)

    @Before
    fun setUp() {
        scanStore = ScanStore(
            scanRestClient,
            scanSqlUtils,
            threatSqlUtils,
            initCoroutineEngine(),
            mock(),
            buildConfigWrapper,
            dispatcher
        )
    }

    @Test
    fun `success on fetch scan state returns the success`() = test {
        val payload = FetchScanStatePayload(siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(FetchedScanStatePayload(null, siteModel))

        val action = ScanActionBuilder.newFetchScanStateAction(payload)
        scanStore.onAction(action)

        val expected = ScanStore.OnScanStateFetched(ScanAction.FETCH_SCAN_STATE)
        verify(dispatcher).emitChange(expected)
    }

    @Test
    fun `error on fetch scan state returns the error`() = test {
        val error = ScanStateError(ScanStateErrorType.INVALID_RESPONSE, "error")
        val payload = FetchedScanStatePayload(error, siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newFetchScanStateAction(FetchScanStatePayload(siteModel))
        scanStore.onAction(fetchAction)

        val expectedEventWithError = ScanStore.OnScanStateFetched(payload.error, ScanAction.FETCH_SCAN_STATE)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `fetch scan state replaces state and threats for site in the db on success`() = test {
        val scanStateModel = mock<ScanStateModel>()
        val payload = FetchedScanStatePayload(scanStateModel, siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(payload)
        whenever(scanStateModel.threats).thenReturn(listOf(threatInCurrentState))

        val fetchAction = ScanActionBuilder.newFetchScanStateAction(FetchScanStatePayload(siteModel))
        scanStore.onAction(fetchAction)

        verify(scanSqlUtils).replaceScanState(siteModel, scanStateModel)
        verify(threatSqlUtils).removeThreatsWithStatus(siteModel, listOf(CURRENT))
        verify(threatSqlUtils).insertThreats(siteModel, listOf(threatInCurrentState))
        val expectedChangeEvent = ScanStore.OnScanStateFetched(ScanAction.FETCH_SCAN_STATE)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun `fetch scan state filters out threats which do not have CURRENT status`() = test {
        whenever(buildConfigWrapper.isDebug()).thenReturn(false)
        val threatsInResponse = listOf(
                threatInCurrentState,
                threatInFixedState
        )
        val expectedThreatsInDb = listOf(threatsInResponse[0])
        val scanStateModel = mock<ScanStateModel>()
        val payload = FetchedScanStatePayload(scanStateModel, siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(payload)
        whenever(scanStateModel.threats).thenReturn(threatsInResponse)

        val fetchAction = ScanActionBuilder.newFetchScanStateAction(FetchScanStatePayload(siteModel))
        scanStore.onAction(fetchAction)

        verify(threatSqlUtils).insertThreats(siteModel, expectedThreatsInDb)
    }

    @Test
    fun `get scan state returns state and threats from the db`() = test {
        val scanStateModel = ScanStateModel(
                state = State.IDLE,
                hasCloud = true,
                threats = listOf(threatInCurrentState),
                reason = Reason.NO_REASON
        )
        whenever(scanSqlUtils.getScanStateForSite(siteModel)).thenReturn(scanStateModel)
        whenever(threatSqlUtils.getThreats(siteModel, listOf(CURRENT))).thenReturn(listOf(threatInCurrentState))

        val scanStateFromDb = scanStore.getScanStateForSite(siteModel)

        verify(scanSqlUtils).getScanStateForSite(siteModel)
        verify(threatSqlUtils).getThreats(siteModel, listOf(CURRENT))
        assertEquals(scanStateModel, scanStateFromDb)
    }

    @Test
    fun `get valid credentials status returns corresponding status from the db`() = test {
        val expectedHasValidCredentials = true
        val scanStateModel = ScanStateModel(
                state = State.IDLE,
                hasValidCredentials = expectedHasValidCredentials,
                reason = Reason.NO_REASON
        )
        whenever(scanSqlUtils.getScanStateForSite(siteModel)).thenReturn(scanStateModel)

        val hasValidCredentials = scanStore.hasValidCredentials(siteModel)

        assertEquals(expectedHasValidCredentials, hasValidCredentials)
    }

    @Test
    fun `success on start scan returns the success`() = test {
        val payload = ScanStartPayload(siteModel)
        whenever(scanRestClient.startScan(siteModel)).thenReturn(ScanStartResultPayload(siteModel))

        val action = ScanActionBuilder.newStartScanAction(payload)
        scanStore.onAction(action)

        val expected = ScanStore.OnScanStarted(ScanAction.START_SCAN)
        verify(dispatcher).emitChange(expected)
    }

    @Test
    fun `error on start scan returns the error`() = test {
        val error = ScanStartError(ScanStartErrorType.GENERIC_ERROR, "error")
        val payload = ScanStartResultPayload(error, siteModel)
        whenever(scanRestClient.startScan(siteModel)).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newStartScanAction(ScanStartPayload(siteModel))
        scanStore.onAction(fetchAction)

        val expectedEventWithError = ScanStore.OnScanStarted(payload.error, ScanAction.START_SCAN)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `success on fix threats return the success`() = test {
        val payload = FixThreatsPayload(siteId, threatIds)
        whenever(scanRestClient.fixThreats(siteId, threatIds)).thenReturn(
            FixThreatsResultPayload(siteId)
        )

        val action = ScanActionBuilder.newFixThreatsAction(payload)
        scanStore.onAction(action)

        val expected = ScanStore.OnFixThreatsStarted(ScanAction.FIX_THREATS)
        verify(dispatcher).emitChange(expected)
    }

    @Test
    fun `error on fix threats returns the error`() = test {
        val error = FixThreatsError(FixThreatsErrorType.GENERIC_ERROR, "error")
        val payload = FixThreatsResultPayload(error, siteId)
        whenever(scanRestClient.fixThreats(siteId, threatIds)).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newFixThreatsAction(FixThreatsPayload(siteId, threatIds))
        scanStore.onAction(fetchAction)

        val expectedEventWithError = ScanStore.OnFixThreatsStarted(payload.error, ScanAction.FIX_THREATS)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `success on ignore threat returns the success`() = test {
        val payload = IgnoreThreatPayload(siteId, threatId)
        whenever(scanRestClient.ignoreThreat(siteId, threatId)).thenReturn(
            IgnoreThreatResultPayload(siteId)
        )

        val action = ScanActionBuilder.newIgnoreThreatAction(payload)
        scanStore.onAction(action)

        val expected = ScanStore.OnIgnoreThreatStarted(ScanAction.IGNORE_THREAT)
        verify(dispatcher).emitChange(expected)
    }

    @Test
    fun `error on ignore threat returns the error`() = test {
        val error = IgnoreThreatError(IgnoreThreatErrorType.GENERIC_ERROR, "error")
        val payload = IgnoreThreatResultPayload(error, siteId)
        whenever(scanRestClient.ignoreThreat(siteId, threatId)).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newIgnoreThreatAction(IgnoreThreatPayload(siteId, threatId))
        scanStore.onAction(fetchAction)

        val expectedEventWithError = ScanStore.OnIgnoreThreatStarted(payload.error, ScanAction.IGNORE_THREAT)
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `success on fetch fix threats status returns the success`() = test {
        val payload = FetchFixThreatsStatusPayload(siteId, listOf(threatId))
        val resultPayload = FetchFixThreatsStatusResultPayload(siteId, mock())
        whenever(scanRestClient.fetchFixThreatsStatus(siteId, listOf(threatId))).thenReturn(resultPayload)

        val action = ScanActionBuilder.newFetchFixThreatsStatusAction(payload)
        scanStore.onAction(action)

        val expected = ScanStore.OnFixThreatsStatusFetched(
            siteId,
            resultPayload.fixThreatStatusModels,
            ScanAction.FETCH_FIX_THREATS_STATUS
        )
        verify(dispatcher).emitChange(expected)
    }

    @Test
    fun `error on fetch fix threats status returns the error`() = test {
        val error = FixThreatsStatusError(FixThreatsStatusErrorType.GENERIC_ERROR, "error")
        val payload = FetchFixThreatsStatusResultPayload(siteId, mock(), error)
        whenever(scanRestClient.fetchFixThreatsStatus(siteId, listOf(threatId))).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newFetchFixThreatsStatusAction(
            FetchFixThreatsStatusPayload(siteId, listOf(threatId))
        )
        scanStore.onAction(fetchAction)

        val expectedEventWithError = ScanStore.OnFixThreatsStatusFetched(
            siteId,
            payload.error,
            ScanAction.FETCH_FIX_THREATS_STATUS
        )
        verify(dispatcher).emitChange(expectedEventWithError)
    }

    @Test
    fun `getScanHistoryForSite returns only FIXED and IGNORED threats`() = test {
        val captor = argumentCaptor<List<ThreatStatus>>()
        whenever(threatSqlUtils.getThreats(anyOrNull(), captor.capture())).thenReturn(mock())

        scanStore.getScanHistoryForSite(siteModel)

        assertThat(captor.firstValue).isEqualTo(listOf(IGNORED, FIXED))
    }
}
