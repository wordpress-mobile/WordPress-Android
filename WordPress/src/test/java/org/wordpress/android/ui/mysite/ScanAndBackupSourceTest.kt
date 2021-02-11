package org.wordpress.android.ui.mysite

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.BackupScreenFeatureConfig
import org.wordpress.android.util.config.ScanScreenFeatureConfig

class ScanAndBackupSourceTest : BaseUnitTest() {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var scanScreenFeatureConfig: ScanScreenFeatureConfig
    @Mock lateinit var backupScreenFeatureConfig: BackupScreenFeatureConfig
    @Mock lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Mock lateinit var site: SiteModel
    @Mock lateinit var siteUtilsWrapper: SiteUtilsWrapper
    private lateinit var scanAndBackupSource: ScanAndBackupSource
    private val siteId = 1
    private val siteRemoteId = 2L

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        scanAndBackupSource = ScanAndBackupSource(
                TEST_DISPATCHER,
                selectedSiteRepository,
                scanScreenFeatureConfig,
                backupScreenFeatureConfig,
                jetpackCapabilitiesUseCase,
                siteUtilsWrapper
        )
        whenever(siteUtilsWrapper.isBackupEnabled(anyBoolean(), anyBoolean())).thenCallRealMethod()
        whenever(siteUtilsWrapper.isScanEnabled(anyBoolean(), anyBoolean(), eq(site))).thenCallRealMethod()
        whenever(site.id).thenReturn(siteId)
        whenever(site.isWPCom).thenReturn(false)
        whenever(site.isWPComAtomic).thenReturn(false)
    }

    @Test
    fun `jetpack capabilities disabled when site not present`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities disabled when both scan and flag are disabled`() = test {
        init(scanScreenFeatureEnabled = false, backupScreenFeatureEnabled = false)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities reloads just scan available when scan flag is enabled`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = true)) }
        )

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isTrue
    }

    @Test
    fun `jetpack capabilities reloads just backup available when backup flag is enabled`() = test {
        init(backupScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = true)) }
        )

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isTrue
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as true`() = test {
        init(scanScreenFeatureEnabled = true, backupScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = true)) }
        )

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isTrue
        assertThat(result!!.scanAvailable).isTrue
    }

    @Test
    fun `jetpack capabilities reloads both scan and backup as false`() = test {
        init(scanScreenFeatureEnabled = true, backupScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = false, backup = false)) }
        )

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.backupAvailable).isFalse
        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on wpcom sites even when Scan product is available`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = false)) }
        )
        whenever(site.isWPCom).thenReturn(true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan not visible on atomic sites even when Scan product is available`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = false)) }
        )
        whenever(site.isWPComAtomic).thenReturn(true)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.scanAvailable).isFalse
    }

    @Test
    fun `Scan visible on non-wpcom sites when Scan product is available and feature flag enabled`() = test {
        init(scanScreenFeatureEnabled = true)
        whenever(site.siteId).thenReturn(siteRemoteId)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(siteRemoteId)).thenReturn(
                flow { emit(JetpackPurchasedProducts(scan = true, backup = false)) }
        )
        whenever(site.isWPCom).thenReturn(false)
        whenever(site.isWPComAtomic).thenReturn(false)

        var result: JetpackCapabilities? = null
        scanAndBackupSource.buildSource(TEST_SCOPE, siteId).observeForever { result = it }

        assertThat(result!!.scanAvailable).isTrue
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
