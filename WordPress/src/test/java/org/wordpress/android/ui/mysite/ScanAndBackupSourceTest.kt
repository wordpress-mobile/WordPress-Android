package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.util.config.BackupScreenFeatureConfig
import org.wordpress.android.util.config.ScanScreenFeatureConfig

class ScanAndBackupSourceTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var scanScreenFeatureConfig: ScanScreenFeatureConfig
    @Mock lateinit var backupScreenFeatureConfig: BackupScreenFeatureConfig
    @Mock lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Mock lateinit var site: SiteModel
    private lateinit var scanAndBackupSource: ScanAndBackupSource
    private val siteId = 1
    private val siteRemoteId = 2L

    @Before
    fun setUp() {
        scanAndBackupSource = ScanAndBackupSource(
                selectedSiteRepository,
                scanScreenFeatureConfig,
                backupScreenFeatureConfig,
                jetpackCapabilitiesUseCase
        )
        whenever(site.id).thenReturn(siteId)
    }

    @Test
    fun `jetpack capabilities disabled when site not present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val result = scanAndBackupSource.buildSource(siteId).single()

        assertThat(result.backupAvailable).isFalse
        assertThat(result.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities disabled when both scan and flag are disabled`() = test {
        init(scanScreenFeatureEnabled = false, backupScreenFeatureEnabled = false)

        val result = scanAndBackupSource.buildSource(siteId).single()

        assertThat(result.backupAvailable).isFalse
        assertThat(result.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities reloads just scan available when scan flag is enabled`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                JetpackPurchasedProducts(scan = true, backup = true)
        )

        val result = scanAndBackupSource.buildSource(siteId).take(2).toList().last()

        assertThat(result.backupAvailable).isFalse
        assertThat(result.scanAvailable).isTrue
    }

    @Test
    fun `jetpack capabilities reloads just backup available when backup flag is enabled`() = test {
        init(backupScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                JetpackPurchasedProducts(scan = true, backup = true)
        )

        val result = scanAndBackupSource.buildSource(siteId).take(2).toList().last()

        assertThat(result.backupAvailable).isTrue
        assertThat(result.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as true`() = test {
        init(scanScreenFeatureEnabled = true, backupScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                JetpackPurchasedProducts(scan = true, backup = true)
        )

        val result = scanAndBackupSource.buildSource(siteId).take(2).toList().last()

        assertThat(result.backupAvailable).isTrue
        assertThat(result.scanAvailable).isTrue
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as false`() = test {
        init(scanScreenFeatureEnabled = true, backupScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                JetpackPurchasedProducts(scan = false, backup = false)
        )

        val result = scanAndBackupSource.buildSource(siteId).take(2).toList().last()

        assertThat(result.backupAvailable).isFalse
        assertThat(result.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on wpcom sites even when Scan product is available`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                JetpackPurchasedProducts(scan = true, backup = false)
        )
        whenever(site.isWPCom).thenReturn(true)

        val result = scanAndBackupSource.buildSource(siteId).take(2).toList().last()

        assertThat(result.scanAvailable).isFalse
    }

    @Test
    fun `Scan visible on non-wpcom sites when Scan product is available and feature flag enabled`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                JetpackPurchasedProducts(scan = true, backup = false)
        )
        whenever(site.isWPCom).thenReturn(false)

        val result = scanAndBackupSource.buildSource(siteId).take(2).toList().last()

        assertThat(result.scanAvailable).isTrue
    }

    private fun init(scanScreenFeatureEnabled: Boolean = false, backupScreenFeatureEnabled: Boolean = false) {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        if (scanScreenFeatureEnabled) {
            whenever(scanScreenFeatureConfig.isEnabled()).thenReturn(scanScreenFeatureEnabled)
        }
        if (backupScreenFeatureEnabled) {
            whenever(backupScreenFeatureConfig.isEnabled()).thenReturn(backupScreenFeatureEnabled)
        }
    }
}
