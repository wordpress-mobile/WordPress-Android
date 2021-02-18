package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.BackupScreenFeatureConfig
import org.wordpress.android.util.config.ScanScreenFeatureConfig
import javax.inject.Inject
import javax.inject.Named

class ScanAndBackupSource @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val scanScreenFeatureConfig: ScanScreenFeatureConfig,
    private val backupScreenFeatureConfig: BackupScreenFeatureConfig,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val siteUtilsWrapper: SiteUtilsWrapper
) : MySiteSource<JetpackCapabilities> {
    override fun buildSource(coroutineScope: CoroutineScope, siteId: Int): LiveData<JetpackCapabilities> {
        val site = selectedSiteRepository.getSelectedSite()
        if (site != null && site.id == siteId &&
                (scanScreenFeatureConfig.isEnabled() || backupScreenFeatureConfig.isEnabled())) {
            val result = MutableLiveData<JetpackCapabilities>()
            coroutineScope.launch(bgDispatcher) {
                jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                    result.postValue(
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
            }
            return result
        } else {
            return MutableLiveData(JetpackCapabilities(scanAvailable = false, backupAvailable = false))
        }
    }

    fun clear() {
        jetpackCapabilitiesUseCase.clear()
    }
}
