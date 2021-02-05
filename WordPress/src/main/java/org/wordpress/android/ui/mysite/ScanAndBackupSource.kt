package org.wordpress.android.ui.mysite

import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.BackupScreenFeatureConfig
import org.wordpress.android.util.config.ScanScreenFeatureConfig
import javax.inject.Inject

class ScanAndBackupSource @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val scanScreenFeatureConfig: ScanScreenFeatureConfig,
    private val backupScreenFeatureConfig: BackupScreenFeatureConfig,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val siteUtilsWrapper: SiteUtilsWrapper
) : MySiteSource<JetpackCapabilities> {
    override fun buildSource(siteId: Int) = flow {
        emit(JetpackCapabilities(scanAvailable = false, backupAvailable = false))
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null || site.id != siteId) return@flow
        if (scanScreenFeatureConfig.isEnabled() || backupScreenFeatureConfig.isEnabled()) {
            val itemsVisibility = jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId)
            emit(
                    JetpackCapabilities(
                            scanAvailable = siteUtilsWrapper.isScanEnabled(
                                    scanScreenFeatureConfig.isEnabled(),
                                    itemsVisibility.scan,
                                    site
                            ),
                            backupAvailable = siteUtilsWrapper.isBackupEnabled(
                                    backupScreenFeatureConfig.isEnabled(),
                                    itemsVisibility.backup
                            )
                    )
            )
        }
    }
}
