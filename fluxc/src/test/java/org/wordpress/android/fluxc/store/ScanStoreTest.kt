package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ScanActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
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
    fun onFetchScanStateActionCallRestClient() = test {
        val payload = FetchScanStatePayload(siteModel)
        whenever(scanRestClient.fetchScanState(siteModel)).thenReturn(
            FetchedScanStatePayload(
                null,
                siteModel
            )
        )
        val action = ScanActionBuilder.newFetchScanStateAction(payload)
        scanStore.onAction(action)

        verify(scanRestClient).fetchScanState(siteModel)
    }
}
