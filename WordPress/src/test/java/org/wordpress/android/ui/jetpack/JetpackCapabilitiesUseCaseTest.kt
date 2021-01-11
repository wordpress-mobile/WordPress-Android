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
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP_REALTIME
import org.wordpress.android.fluxc.model.JetpackCapability.SCAN
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnJetpackCapabilitiesFetched
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.util.Date

private const val SITE_ID = 1L
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackCapabilitiesUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: SiteStore
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock private lateinit var currentTimeProvider: CurrentTimeProvider
    private lateinit var useCase: JetpackCapabilitiesUseCase
    private lateinit var event: OnJetpackCapabilitiesFetched

    @Before
    fun setUp() {
        useCase = JetpackCapabilitiesUseCase(
                store,
                dispatcher,
                appPrefsWrapper,
                currentTimeProvider,
                TEST_DISPATCHER
        )
        event = OnJetpackCapabilitiesFetched(SITE_ID, listOf(BACKUP_REALTIME), null)
        whenever(appPrefsWrapper.getSiteJetpackCapabilitiesLastUpdated(anyLong())).thenReturn(0)
        whenever(currentTimeProvider.currentDate).thenReturn(Date(99999999))
    }

    @Test
    fun `coroutine resumed, when result event dispatched`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        val resultEvent = useCase.getOrFetchJetpackCapabilities(SITE_ID)

        assertThat(resultEvent).isEqualTo(event.capabilities)
    }

    @Test
    fun `useCase subscribes to event bus`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        useCase.getOrFetchJetpackCapabilities(SITE_ID)

        verify(dispatcher).register(useCase)
    }

    @Test
    fun `useCase unsubscribes from event bus`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        useCase.getOrFetchJetpackCapabilities(SITE_ID)

        verify(dispatcher).unregister(useCase)
    }

    @Test
    fun `cached value used, when not older than MAX_CACHE_VALIDITY`() = test {
        val expected = listOf(BACKUP, SCAN)
        whenever(currentTimeProvider.currentDate).thenReturn(Date(MAX_CACHE_VALIDITY - 1))
        whenever(appPrefsWrapper.getSiteJetpackCapabilities(SITE_ID)).thenReturn(expected)

        val result = useCase.getOrFetchJetpackCapabilities(SITE_ID)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `fetch invoked, when older than MAX_CACHE_VALIDITY`() = test {
        whenever(currentTimeProvider.currentDate).thenReturn(Date(MAX_CACHE_VALIDITY))
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        val result = useCase.getOrFetchJetpackCapabilities(SITE_ID)

        assertThat(result).isEqualTo(event.capabilities)
    }

    @Test
    fun `updates cache, when fetch finishes`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        val result = useCase.getOrFetchJetpackCapabilities(SITE_ID)

        verify(appPrefsWrapper).setSiteJetpackCapabilities(SITE_ID, event.capabilities!!)
    }
}
