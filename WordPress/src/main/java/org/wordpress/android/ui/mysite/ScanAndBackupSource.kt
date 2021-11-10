package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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
    val refresh: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    override fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<JetpackCapabilities> {
        val result = MediatorLiveData<JetpackCapabilities>()
        result.refreshData(coroutineScope, siteLocalId, false)
        result.addSource(refresh) {
            if (refresh.value == true) {
                result.refreshData(coroutineScope, siteLocalId, true)
            }
        }
        return result
    }

    fun refresh() {
        refresh.postValue(true)
    }

    private fun MediatorLiveData<JetpackCapabilities>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            coroutineScope.launch(bgDispatcher) {
                jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(selectedSite.siteId).collect {
                    postValues(
                            JetpackCapabilities(
                                    scanAvailable = SiteUtils.isScanEnabled(it.scan, selectedSite),
                                    backupAvailable = it.backup
                            ), isRefresh
                    )
                }
            }
        } else {
            postValues(JetpackCapabilities(scanAvailable = false, backupAvailable = false), isRefresh)
        }
    }

    private fun MediatorLiveData<JetpackCapabilities>.postValues(
        jetpackCapabilities: JetpackCapabilities,
        isRefresh: Boolean
    ) {
        if (isRefresh) refresh.postValue(false)
        this@postValues.postValue(jetpackCapabilities)
    }

    fun clear() {
        jetpackCapabilitiesUseCase.clear()
    }
}
