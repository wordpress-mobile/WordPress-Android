package org.wordpress.android.ui.sitecreation.usecases

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.test

private const val SEARCH_QUERY = "test"

@RunWith(MockitoJUnitRunner::class)
class FetchVerticalsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var store: VerticalStore
    private lateinit var useCase: FetchVerticalsUseCase
    private lateinit var dispatchCaptor: KArgumentCaptor<Action<FetchVerticalsPayload>>
    private val event = OnVerticalsFetched(SEARCH_QUERY, emptyList(), null)

    @Before
    fun setUp() {
        useCase = FetchVerticalsUseCase(dispatcher, store)
        dispatchCaptor = argumentCaptor()
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onVerticalsFetched(event) }

        val resultEvent = useCase.fetchVerticals(SEARCH_QUERY)

        verify(dispatcher).dispatch(dispatchCaptor.capture())
        assertEquals(dispatchCaptor.lastValue.payload.searchQuery, SEARCH_QUERY)
        assertEquals(event, resultEvent)
    }
}
