package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities

@ExperimentalCoroutinesApi
class ScanAndBackupSourceTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    @Mock
    lateinit var site: SiteModel
    private lateinit var scanAndBackupSource: ScanAndBackupSource
    private val siteLocalId = 1
    private val siteRemoteId = 2L
    private lateinit var isRefreshing: MutableList<Boolean>

    @Before
    fun setUp() {
        whenever(site.id).thenReturn(siteLocalId)
        whenever(site.isWPCom).thenReturn(false)
        whenever(site.isWPComAtomic).thenReturn(false)
        isRefreshing = mutableListOf()
    }

    @Test
    fun `jetpack capabilities disabled when site not present`() = test {
        initScanAndBackupSource(hasSelectedSite = false)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.build(testScope(), siteLocalId).observeForever {
            result = it
        }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as true`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.build(testScope(), siteLocalId).observeForever {
            result = it
        }

        assertThat(result!!.backupAvailable).isTrue
        assertThat(result!!.scanAvailable).isTrue
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as false`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = false, backupPurchased = false)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.build(testScope(), siteLocalId).observeForever {
            result = it
        }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on wpcom sites even when Scan product is available`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = false)
        whenever(site.isWPCom).thenReturn(true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.build(testScope(), siteLocalId).observeForever {
            result = it
        }

        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on atomic sites even when Scan product is available`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = false)
        whenever(site.isWPComAtomic).thenReturn(true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.build(testScope(), siteLocalId).observeForever {
            result = it
        }

        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan visible on non-wpcom sites when Scan product is available and feature flag enabled`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = false)
        whenever(site.isWPCom).thenReturn(false)
        whenever(site.isWPComAtomic).thenReturn(false)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.build(testScope(), siteLocalId).observeForever {
            result = it
        }

        assertThat(result!!.scanAvailable).isTrue
    }

    @Test
    fun `when refresh is invoked, then data is refreshed`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = true)

        scanAndBackupSource.build(testScope(), siteLocalId).observeForever { }
        scanAndBackupSource.refresh.observeForever { isRefreshing.add(it) }

        scanAndBackupSource.refresh()

        verify(jetpackCapabilitiesUseCase, times(2)).getJetpackPurchasedProducts(any())
    }

    @Test
    fun `when build is invoked, then refresh is true`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = true)
        scanAndBackupSource.refresh.observeForever { isRefreshing.add(it) }

        scanAndBackupSource.build(testScope(), siteLocalId)

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when refresh is invoked, then refresh is true`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = true)
        scanAndBackupSource.refresh.observeForever { isRefreshing.add(it) }

        scanAndBackupSource.refresh()

        assertThat(isRefreshing.last()).isTrue
    }

    @Test
    fun `when data has been refreshed, then refresh is set to false`() = test {
        initScanAndBackupSource(hasSelectedSite = true, scanPurchased = true, backupPurchased = false)

        scanAndBackupSource.build(testScope(), siteLocalId).observeForever { }
        scanAndBackupSource.refresh.observeForever { isRefreshing.add(it) }

        scanAndBackupSource.refresh()

        assertThat(isRefreshing.last()).isFalse
    }

    private suspend fun initScanAndBackupSource(
        hasSelectedSite: Boolean = true,
        scanPurchased: Boolean = false,
        backupPurchased: Boolean = false
    ) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(if (hasSelectedSite) site else null)
        if (hasSelectedSite) {
            whenever(site.siteId).thenReturn(siteRemoteId)
            whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = scanPurchased, backup = backupPurchased)) }
            )
        }
        scanAndBackupSource = ScanAndBackupSource(
            testDispatcher(),
            selectedSiteRepository,
            jetpackCapabilitiesUseCase
        )
    }
}
