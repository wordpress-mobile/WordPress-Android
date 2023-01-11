package org.wordpress.android.ui.jetpack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP_DAILY
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP_REALTIME
import org.wordpress.android.fluxc.model.JetpackCapability.SCAN
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnJetpackCapabilitiesFetched
import org.wordpress.android.ui.prefs.AppPrefsWrapper

private const val SITE_ID = 1L

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class JetpackCapabilitiesUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var store: SiteStore

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var useCase: JetpackCapabilitiesUseCase
    private lateinit var event: OnJetpackCapabilitiesFetched

    @Before
    fun setUp() {
        useCase = JetpackCapabilitiesUseCase(
            store,
            dispatcher,
            appPrefsWrapper
        )
        event = buildCapabilitiesFetchedEvent(listOf(BACKUP_REALTIME))
    }

    @Test
    fun `coroutine resumed, when result event dispatched`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf())

        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `when purchased products requested, then data from cache are returned first`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }
        whenever(appPrefsWrapper.getSiteJetpackCapabilities(SITE_ID)).thenReturn(mutableListOf(SCAN))

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf())

        verify(appPrefsWrapper).getSiteJetpackCapabilities(SITE_ID)
        assertThat(result.first().scan).isTrue
    }

    @Test
    fun `when purchased products requested, then data from server are returned`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf(BACKUP)))
        }
        whenever(appPrefsWrapper.getSiteJetpackCapabilities(SITE_ID)).thenReturn(mutableListOf(SCAN))

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf())

        verify(appPrefsWrapper).getSiteJetpackCapabilities(SITE_ID)
        assertThat(result.last().backup).isTrue
    }

    @Test
    fun `updates cache, when fetch finishes`() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onJetpackCapabilitiesFetched(event) }

        useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf())

        verify(appPrefsWrapper).setSiteJetpackCapabilities(SITE_ID, event.capabilities)
    }

    @Test
    fun `Scan enabled, when JetpackCapabilities contains SCAN`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf(SCAN)))
        }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf()).last()

        assertThat(result.scan).isTrue
    }

    @Test
    fun `Scan disabled, when JetpackCapabilities does NOT contain SCAN`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf()))
        }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf()).last()

        assertThat(result.scan).isFalse
    }

    @Test
    fun `Backup enabled, when JetpackCapabilities contains BACKUP`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf(BACKUP)))
        }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf()).last()

        assertThat(result.backup).isTrue
    }

    @Test
    fun `Backup enabled, when JetpackCapabilities contains BACKUP_REALTIME`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf(BACKUP_REALTIME)))
        }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf()).last()

        assertThat(result.backup).isTrue
    }

    @Test
    fun `Backup enabled, when JetpackCapabilities contains BACKUP_DAILY`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf(BACKUP_DAILY)))
        }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf()).last()

        assertThat(result.backup).isTrue
    }

    @Test
    fun `Backup disabled, when JetpackCapabilities does not contain any BACKUP capability`() = test {
        whenever(dispatcher.dispatch(any())).then {
            useCase.onJetpackCapabilitiesFetched(buildCapabilitiesFetchedEvent(listOf()))
        }

        val result = useCase.getJetpackPurchasedProducts(SITE_ID).toList(mutableListOf()).last()

        assertThat(result.backup).isFalse
    }

    private fun buildCapabilitiesFetchedEvent(capabilities: List<JetpackCapability>) =
        OnJetpackCapabilitiesFetched(SITE_ID, capabilities, null)
}
