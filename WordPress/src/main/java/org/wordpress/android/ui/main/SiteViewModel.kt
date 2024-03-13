package org.wordpress.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.main.utils.SiteRecordUtil
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore
) : ScopedViewModel(mainDispatcher) {
    private val _sites = MutableLiveData<List<SiteRecord>>()
    val sites: LiveData<List<SiteRecord>> = _sites

    fun loadSites() = launch {
        (siteStore.visibleSitesAccessedViaWPCom + siteStore.sitesAccessedViaXMLRPC)
            .let { SiteRecordUtil.createRecords(it) }
            .sortedBy { it.blogNameOrHomeURL }
            .let { _sites.postValue(it) }
    }
}
