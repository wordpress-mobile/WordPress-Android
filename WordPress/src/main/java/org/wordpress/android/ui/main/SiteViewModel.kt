package org.wordpress.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SiteViewModel @Inject constructor(
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(bgDispatcher) {
    private val _sites = MutableLiveData<List<SiteRecord>>()
    val sites: LiveData<List<SiteRecord>> = _sites

    fun loadSites(sitePickerMode: SitePickerMode) = launch {
        _sites.postValue(getSites(sitePickerMode))
    }

    fun searchSites(keyword: String, sitePickerMode: SitePickerMode) = launch {
        if (keyword.trim().isEmpty()) {
            _sites.postValue(getSites(sitePickerMode))
            return@launch
        }
        getSites(sitePickerMode).filter { record ->
            val siteName: String = record.blogName.lowercase(Locale.getDefault())
            val hostName: String = record.homeURL.lowercase(Locale.ROOT)

            siteName.contains(keyword.lowercase(Locale.getDefault())) ||
                    hostName.contains(keyword.lowercase(Locale.ROOT))
        }.let { _sites.postValue(it) }
    }

    /**
     * Returns a list of sites to display in the site picker.
     * @param mode if [SitePickerMode.WPCOM_SITES_ONLY], only WPCOM sites are returned.
     */
    private fun getSites(mode: SitePickerMode): List<SiteRecord> {
        val result = if (mode == SitePickerMode.WPCOM_SITES_ONLY) {
            siteStore.sitesAccessedViaWPComRest
        } else {
            siteStore.sites
        }
        return result.map { SiteRecord(it) }.let { sortSites(it) }
    }

    private fun sortSites(records: List<SiteRecord>): List<SiteRecord> {
        val sites = records.toMutableList()
        val pinnedSites = appPrefsWrapper.pinnedSiteLocalIds
            .mapNotNull { pinnedId -> sites.firstOrNull { it.localId == pinnedId } }

        pinnedSites.forEach { sites.remove(it) }

        return pinnedSites + sites.sortedBy { it.blogNameOrHomeURL }
    }
}
