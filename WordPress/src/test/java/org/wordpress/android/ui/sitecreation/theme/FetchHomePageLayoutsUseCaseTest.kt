package org.wordpress.android.ui.sitecreation.theme

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase

@RunWith(MockitoJUnitRunner::class)
class FetchHomePageLayoutsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var store: ThemeStore
    @Mock lateinit var thumbDimensionProvider: ThumbDimensionProvider

    private lateinit var useCase: FetchHomePageLayoutsUseCase
    private lateinit var dispatchCaptor: KArgumentCaptor<Action<SuggestDomainsPayload>>
    private val event = OnStarterDesignsFetched(emptyList(), null)

    @Before
    fun setUp() {
        useCase = FetchHomePageLayoutsUseCase(dispatcher, store, thumbDimensionProvider)
        dispatchCaptor = argumentCaptor()
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onStarterDesignsFetched(event) }
        val resultEvent = useCase.fetchStarterDesigns()
        verify(dispatcher).dispatch(dispatchCaptor.capture())
        Assert.assertEquals(event, resultEvent)
    }
}
