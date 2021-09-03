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
import org.wordpress.android.util.SiteUtils
import javax.inject.Inject
import javax.inject.Named

class ScanAndBackupSource @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
) : MySiteSource<JetpackCapabilities> {
    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<JetpackCapabilities> {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            val result = MutableLiveData<JetpackCapabilities>()
            coroutineScope.launch(bgDispatcher) {
                jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(selectedSite.siteId).collect {
                    result.postValue(
                            JetpackCapabilities(
                                    scanAvailable = SiteUtils.isScanEnabled(it.scan, selectedSite),
                                    backupAvailable = it.backup
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
