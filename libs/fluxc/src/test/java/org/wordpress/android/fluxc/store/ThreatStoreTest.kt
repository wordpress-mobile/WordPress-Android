package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ThreatAction
import org.wordpress.android.fluxc.generated.ThreatActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.ThreatRestClient
import org.wordpress.android.fluxc.store.ThreatStore.FetchThreatPayload
import org.wordpress.android.fluxc.store.ThreatStore.FetchedThreatPayload
import org.wordpress.android.fluxc.store.ThreatStore.ThreatError
import org.wordpress.android.fluxc.store.ThreatStore.ThreatErrorType
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class ThreatStoreTest {
    @Mock private lateinit var threatRestClient: ThreatRestClient
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var threatModel: ThreatModel
    private lateinit var threatStore: ThreatStore

    @Before
    fun setUp() {
        threatStore = ThreatStore(
            threatRestClient,
            initCoroutineEngine(),
            dispatcher
        )
    }

    @Test
    fun `fetch threat triggers rest client`() = test {
        val payload = FetchThreatPayload(siteModel, threatModel)
        whenever(threatRestClient.fetchThreat(siteModel, threatModel)).thenReturn(FetchedThreatPayload(null, siteModel))

        val action = ThreatActionBuilder.newFetchThreatAction(payload)
        threatStore.onAction(action)

        verify(threatRestClient).fetchThreat(siteModel, threatModel)
    }

    @Test
    fun `error on fetch threat returns the error`() = test {
        val error = ThreatError(ThreatErrorType.INVALID_RESPONSE, "error")

        val payload = FetchedThreatPayload(error, siteModel)
        whenever(threatRestClient.fetchThreat(siteModel, threatModel)).thenReturn(payload)

        val fetchAction = ThreatActionBuilder.newFetchThreatAction(FetchThreatPayload(siteModel, threatModel))
        threatStore.onAction(fetchAction)

        val expectedEventWithError = ThreatStore.OnThreatFetched(payload.error, ThreatAction.FETCH_THREAT)
        verify(dispatcher).emitChange(expectedEventWithError)
    }
}
