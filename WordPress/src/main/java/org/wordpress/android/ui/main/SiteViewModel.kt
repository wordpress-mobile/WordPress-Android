package org.wordpress.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _sites = MutableLiveData<List<SiteRecord>>()
    val sites: LiveData<List<SiteRecord>> = _sites

    fun loadSites() = launch {
        val sites = (siteStore.visibleSitesAccessedViaWPCom + siteStore.sitesAccessedViaXMLRPC)
            .map { SiteRecord(it) }
            .toMutableList()

        val pinnedSites = appPrefsWrapper.pinnedSiteLocalIds
            .mapNotNull { pinnedId-> sites.firstOrNull { it.localId == pinnedId} }

        pinnedSites.forEach { sites.remove(it) }

        _sites.postValue(pinnedSites + sites.sortedBy { it.blogNameOrHomeURL })
    }
}
