package org.wordpress.android.ui.sitecreation.usecases

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptPayload
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class FetchSegmentsPromptUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var store: VerticalStore
    private lateinit var useCase: FetchSegmentPromptUseCase
    private lateinit var dispatchCaptor: KArgumentCaptor<Action<FetchSegmentPromptPayload>>
    private val event = OnSegmentPromptFetched(1L, SegmentPromptModel("", "", ""), null)

    @Before
    fun setUp() {
        useCase = FetchSegmentPromptUseCase(dispatcher, store)
        dispatchCaptor = argumentCaptor()
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onSegmentPromptFetched(event) }

        val resultEvent = useCase.fetchSegmentsPrompt(1L)

        verify(dispatcher).dispatch(dispatchCaptor.capture())
        Assert.assertEquals(1L, dispatchCaptor.lastValue.payload.segmentId)
        Assert.assertEquals(event, resultEvent)
    }
}
