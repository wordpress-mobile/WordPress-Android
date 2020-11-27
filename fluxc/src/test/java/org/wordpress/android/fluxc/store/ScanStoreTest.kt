package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ScanAction
import org.wordpress.android.fluxc.generated.ScanActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartError
import org.wordpress.android.fluxc.store.ScanStore.ScanStartErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ScanStore.ScanStartPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartResultPayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStateError
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class ScanStoreTest {
    @Mock private lateinit var scanRestClient: ScanRestClient
    @Mock private lateinit var scanSqlUtils: ScanSqlUtils
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    private lateinit var scanStore: ScanStore

    @Before
    fun setUp() {
        scanStore = ScanStore(
            scanRestClient,
            scanSqlUtils,
            initCoroutineEngine(),
            dispatcher
        )
    }

    @Test
    fun `fetch scan state triggers rest client`() = test {
        val payload = FetchScanStatePayload(siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(FetchedScanStatePayload(null, siteModel))

        val action = ScanActionBuilder.newFetchScanStateAction(payload)
        scanStore.onAction(action)

        verify(scanRestClient).fetchScanState(siteModel)
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
    fun `fetch scan state stores state in the db on success`() = test {
        val scanStateModel = mock<ScanStateModel>()
        val payload = FetchedScanStatePayload(scanStateModel, siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newFetchScanStateAction(FetchScanStatePayload(siteModel))
        scanStore.onAction(fetchAction)

        verify(scanSqlUtils).replaceScanState(siteModel, scanStateModel)
        val expectedChangeEvent = ScanStore.OnScanStateFetched(ScanAction.FETCH_SCAN_STATE)
        verify(dispatcher).emitChange(eq(expectedChangeEvent))
    }

    @Test
    fun `get scan state returns state from the db`() {
        val scanStateModel = mock<ScanStateModel>()
        whenever(scanSqlUtils.getScanStateForSite(siteModel)).thenReturn(scanStateModel)

        val scanStateFromDb = scanStore.getScanStateForSite(siteModel)

        verify(scanSqlUtils).getScanStateForSite(siteModel)
        Assert.assertEquals(scanStateModel, scanStateFromDb)
    }

    @Test
    fun `start scan triggers rest client`() = test {
        val payload = ScanStartPayload(siteModel)
        whenever(scanRestClient.startScan(siteModel)).thenReturn(ScanStartResultPayload(siteModel))

        val action = ScanActionBuilder.newStartScanAction(payload)
        scanStore.onAction(action)

        verify(scanRestClient).startScan(siteModel)
    }

    @Test
    fun `error on start scan returns the error`() = test {
        val error = ScanStartError(GENERIC_ERROR, "error")
        val payload = ScanStartResultPayload(error, siteModel)
        whenever(scanRestClient.startScan(siteModel)).thenReturn(payload)

        val fetchAction = ScanActionBuilder.newStartScanAction(ScanStartPayload(siteModel))
        scanStore.onAction(fetchAction)

        val expectedEventWithError = ScanStore.OnScanStarted(payload.error, ScanAction.START_SCAN)
        verify(dispatcher).emitChange(expectedEventWithError)
    }
}
