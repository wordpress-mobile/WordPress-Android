package org.wordpress.android.ui.jetpack.scan

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.IDLE
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.UNAVAILABLE
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.OnScanStateFetched
import org.wordpress.android.fluxc.store.ScanStore.ScanStateError
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.test
import org.wordpress.android.util.ScanFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class ScanStatusServiceTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    private val scanStateCaptor = argumentCaptor<FetchScanStatePayload>()

    @Mock private lateinit var scanStore: ScanStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanFeatureConfig: ScanFeatureConfig

    private lateinit var scanStatusService: ScanStatusService
    private var scanAvailable: Boolean? = null

    private val scanStateModel = ScanStateModel(
        state = IDLE,
        reason = null,
        threats = null,
        credentials = null,
        hasCloud = false,
        mostRecentStatus = null,
        currentStatus = null
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        scanStatusService = ScanStatusService(
            scanStore,
            scanFeatureConfig,
            TEST_SCOPE
        )
        scanAvailable = null
        scanStatusService.scanAvailable.observeForever { scanAvailable = it }

        whenever(scanFeatureConfig.isEnabled()).thenReturn(true)
        whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
        whenever(scanStore.fetchScanState(any())).thenReturn(OnScanStateFetched(FETCH_SCAN_STATE))
    }

    @After
    fun tearDown() {
        scanStatusService.stop()
    }

    @Test
    fun emitsScanAvailableOnStartWhenScanIsAvailable() {
        whenever(scanStore.getScanStateForSite(site)).thenReturn(scanStateModel)

        scanStatusService.start(site)

        assertEquals(true, scanAvailable)
    }

    @Test
    fun emitsScanNotAvailableOnStartWhenScanIsNotAvailable() {
        val unknownScanStateModel = scanStateModel.copy(state = UNAVAILABLE)
        whenever(scanStore.getScanStateForSite(site)).thenReturn(unknownScanStateModel)

        scanStatusService.start(site)

        assertEquals(false, scanAvailable)
    }

    @Test
    fun emitsScanNotAvailableOnError() = test {
        whenever(scanStore.fetchScanState(any())).thenReturn(
            OnScanStateFetched(ScanStateError(AUTHORIZATION_REQUIRED), FETCH_SCAN_STATE)
        )
        whenever(scanStore.getScanStateForSite(site)).thenReturn(scanStateModel)

        scanStatusService.start(site)

        assertEquals(false, scanAvailable)
    }

    @Test
    fun triggersFetchOnStart() = runBlocking {
        scanStatusService.start(site)

        assertFetchScanStateAction()
    }

    private suspend fun assertFetchScanStateAction() {
        verify(scanStore).fetchScanState(scanStateCaptor.capture())
        scanStateCaptor.firstValue.apply {
            assertEquals(site, this@ScanStatusServiceTest.site)
        }
    }
}
