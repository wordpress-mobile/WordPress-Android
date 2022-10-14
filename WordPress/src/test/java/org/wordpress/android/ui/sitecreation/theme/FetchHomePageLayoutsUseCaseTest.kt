package org.wordpress.android.ui.sitecreation.theme

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
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
import org.wordpress.android.fluxc.store.ThemeStore.FetchStarterDesignsPayload
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchHomePageLayoutsUseCase.GROUP
import org.wordpress.android.util.config.BetaSiteDesignsFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class FetchHomePageLayoutsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var store: ThemeStore
    @Mock lateinit var thumbDimensionProvider: SiteDesignRecommendedDimensionProvider
    @Mock lateinit var betaSiteDesigns: BetaSiteDesignsFeatureConfig

    private lateinit var useCase: FetchHomePageLayoutsUseCase
    private lateinit var dispatchCaptor: KArgumentCaptor<Action<SuggestDomainsPayload>>
    private val event = OnStarterDesignsFetched(emptyList(), emptyList(), null)

    @Before
    fun setUp() {
        useCase = FetchHomePageLayoutsUseCase(dispatcher, store, thumbDimensionProvider, betaSiteDesigns)
        dispatchCaptor = argumentCaptor()
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onStarterDesignsFetched(event) }
        val resultEvent = useCase.fetchStarterDesigns()
        verify(dispatcher).dispatch(dispatchCaptor.capture())
        Assert.assertEquals(event, resultEvent)
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `when beta site designs are enabled the stable and beta groups are passed to the call`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onStarterDesignsFetched(event) }
        whenever(betaSiteDesigns.isEnabled()).thenReturn(true)
        useCase.fetchStarterDesigns()
        verify(dispatcher).dispatch(dispatchCaptor.capture())
        assertThat(requireNotNull(dispatchCaptor.firstValue.payload as FetchStarterDesignsPayload).groups).isEqualTo(
                arrayOf(
                        GROUP.STABLE.key,
                        GROUP.BETA.key
                )
        )
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun `when beta site designs are disabled no groups are passed to the call`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onStarterDesignsFetched(event) }
        whenever(betaSiteDesigns.isEnabled()).thenReturn(false)
        useCase.fetchStarterDesigns()
        verify(dispatcher).dispatch(dispatchCaptor.capture())
        assertThat(requireNotNull(dispatchCaptor.firstValue.payload as FetchStarterDesignsPayload).groups).isEqualTo(
                emptyArray<String>()
        )
    }
}
