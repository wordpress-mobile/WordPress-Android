package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.testScope
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities

class ScanAndBackupSourceTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Mock lateinit var site: SiteModel
    private lateinit var scanAndBackupSource: ScanAndBackupSource
    private val siteLocalId = 1
    private val siteRemoteId = 2L

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        scanAndBackupSource = ScanAndBackupSource(
                TEST_DISPATCHER,
                selectedSiteRepository,
                jetpackCapabilitiesUseCase
        )
        whenever(site.id).thenReturn(siteLocalId)
        whenever(site.isWPCom).thenReturn(false)
        whenever(site.isWPComAtomic).thenReturn(false)
    }

    @Test
    fun `jetpack capabilities disabled when site not present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as true`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = true)) }
        )

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isTrue
        assertThat(result!!.scanAvailable).isTrue
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as false`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = false, backup = false)) }
        )

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on wpcom sites even when Scan product is available`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = false)) }
        )
        whenever(site.isWPCom).thenReturn(true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on atomic sites even when Scan product is available`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = false)) }
        )
        whenever(site.isWPComAtomic).thenReturn(true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan visible on non-wpcom sites when Scan product is available and feature flag enabled`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = false)) }
        )
        whenever(site.isWPCom).thenReturn(false)
        whenever(site.isWPComAtomic).thenReturn(false)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(testScope(), siteLocalId).observeForever { result = it }

        assertThat(result!!.scanAvailable).isTrue
    }
}
