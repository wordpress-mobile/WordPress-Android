package org.wordpress.android.ui.mysite

import kotlinx.coroutines.flow.collect
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
        val site = selectedSiteRepository.getSelectedSite()
        if (site != null && site.id == siteId &&
                (scanScreenFeatureConfig.isEnabled() || backupScreenFeatureConfig.isEnabled())) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                emit(
                        JetpackCapabilities(
                                scanAvailable = siteUtilsWrapper.isScanEnabled(
                                        scanScreenFeatureConfig.isEnabled(),
                                        it.scan,
                                        site
                                ),
                                backupAvailable = siteUtilsWrapper.isBackupEnabled(
                                        backupScreenFeatureConfig.isEnabled(),
                                        it.backup
                                )
                        )
                )
            }
        } else {
            emit(JetpackCapabilities(scanAvailable = false, backupAvailable = false))
        }
    }
}
