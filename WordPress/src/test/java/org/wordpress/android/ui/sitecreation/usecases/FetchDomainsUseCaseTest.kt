package org.wordpress.android.ui.sitecreation.usecases

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.test

private const val SEARCH_QUERY = "test"

@RunWith(MockitoJUnitRunner::class)
class FetchDomainsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var store: SiteStore
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

        val resultEvent = useCase.fetchDomains(SEARCH_QUERY)

        verify(dispatcher).dispatch(dispatchCaptor.capture())
        assertEquals(dispatchCaptor.lastValue.payload.query, SEARCH_QUERY)
        assertEquals(event, resultEvent)
    }
}
