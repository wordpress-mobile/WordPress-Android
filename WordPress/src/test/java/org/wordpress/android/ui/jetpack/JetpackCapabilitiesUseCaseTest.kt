package org.wordpress.android.ui.jetpack

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnJetpackCapabilitiesFetched
import org.wordpress.android.test

private const val SITE_ID = 1L
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackCapabilitiesUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: SiteStore
    private lateinit var useCase: JetpackCapabilitiesUseCase
    private lateinit var event: OnJetpackCapabilitiesFetched

    @Before
    fun setUp() {
        this@FetchJetpackCapabilitiesUseCaseTest.useCase = JetpackCapabilitiesUseCase(store, dispatcher, TEST_DISPATCHER)
        event = OnJetpackCapabilitiesFetched(SITE_ID, listOf(), null)
    }

    @Test
    fun `coroutine resumed, when result event dispatched`() = test {
        whenever(dispatcher.dispatch(any())).then { this@FetchJetpackCapabilitiesUseCaseTest.useCase.onJetpackCapabilitiesFetched(event) }

        val resultEvent = this@FetchJetpackCapabilitiesUseCaseTest.useCase.fetchJetpackCapabilities(SITE_ID)

        assertThat(resultEvent).isEqualTo(event)
    }

    @Test
    fun `useCase subscribes to event bus`() = test {
        whenever(dispatcher.dispatch(any())).then { this@FetchJetpackCapabilitiesUseCaseTest.useCase.onJetpackCapabilitiesFetched(event) }

        this@FetchJetpackCapabilitiesUseCaseTest.useCase.fetchJetpackCapabilities(SITE_ID)

        verify(dispatcher).register(this@FetchJetpackCapabilitiesUseCaseTest.useCase)
    }

    @Test
    fun `useCase unsubscribes from event bus`() = test {
        whenever(dispatcher.dispatch(any())).then { this@FetchJetpackCapabilitiesUseCaseTest.useCase.onJetpackCapabilitiesFetched(event) }

        this@FetchJetpackCapabilitiesUseCaseTest.useCase.fetchJetpackCapabilities(SITE_ID)

        verify(dispatcher).unregister(this@FetchJetpackCapabilitiesUseCaseTest.useCase)
    }
}
