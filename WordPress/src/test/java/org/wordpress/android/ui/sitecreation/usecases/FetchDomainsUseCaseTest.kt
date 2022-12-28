package org.wordpress.android.ui.sitecreation.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload

private const val SEARCH_QUERY = "test"
private const val SEGMENT_ID = 123L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchDomainsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher
    @Mock
    lateinit var store: SiteStore
    private lateinit var useCase: FetchDomainsUseCase
    private lateinit var dispatchCaptor: KArgumentCaptor<Action<SuggestDomainsPayload>>
    private val event = OnSuggestedDomains(SEARCH_QUERY, emptyList())

    @Before
    fun setUp() {
        useCase = FetchDomainsUseCase(dispatcher, store)
        dispatchCaptor = argumentCaptor()
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onSuggestedDomains(event) }

        val resultEvent = useCase.fetchDomains(SEARCH_QUERY, SEGMENT_ID)

        verify(dispatcher).dispatch(dispatchCaptor.capture())
        assertEquals(dispatchCaptor.lastValue.payload.query, SEARCH_QUERY)
        assertEquals(event, resultEvent)
    }
}
