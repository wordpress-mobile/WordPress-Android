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
        result.refreshData(coroutineScope, siteLocalId)
        result.addSource(refresh) { result.refreshData(coroutineScope, siteLocalId, refresh.value) }
        return result
    }

    fun refresh() {
        refresh.postValue(true)
    }

    private fun MediatorLiveData<JetpackCapabilities>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int,
        isRefresh: Boolean? = null
    ) {
        when (isRefresh) {
            null, true -> refreshData(coroutineScope, siteLocalId)
            false -> Unit // Do nothing
        }
    }

    private fun MediatorLiveData<JetpackCapabilities>.refreshData(
        coroutineScope: CoroutineScope,
        siteLocalId: Int
    ) {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null && selectedSite.id == siteLocalId) {
            coroutineScope.launch(bgDispatcher) {
                jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(selectedSite.siteId).collect {
                    postState(
                            JetpackCapabilities(
                                    scanAvailable = SiteUtils.isScanEnabled(it.scan, selectedSite),
                                    backupAvailable = it.backup
                            )
                    )
                }
            }
        } else {
            postState(JetpackCapabilities(scanAvailable = false, backupAvailable = false))
        }
    }

    private fun MediatorLiveData<JetpackCapabilities>.postState(
        jetpackCapabilities: JetpackCapabilities
    ) {
        refresh.postValue(false)
        this@postState.postValue(jetpackCapabilities)
    }

    fun clear() {
        jetpackCapabilitiesUseCase.clear()
    }
}
