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

    /**
     * Load for sites by keyword.
     * If the keyword is not set, all sites are returned.
     */
    fun loadSites(sitePickerMode: SitePickerMode, keyword: String? = null) = launch {
        if (keyword == null || keyword.trim().isEmpty()) {
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
     * If mode is [SitePickerMode.WPCOM_SITES_ONLY], only WPCOM sites are returned.
     */
    private fun getSites(mode: SitePickerMode): List<SiteRecord> {
        val result = if (mode == SitePickerMode.WPCOM_SITES_ONLY) {
            siteStore.sitesAccessedViaWPComRest
        } else {
            siteStore.sites
        }
        return result.map { SiteRecord(it) }.let { sortSites(it) }
    }

    /**
     * Make the pinned sites appear first in the list, followed by the recent sites.
     * Then sort the list of sites by blog name or home URL.
     */
    private fun sortSites(records: List<SiteRecord>): List<SiteRecord> {
        val allSites = records.toMutableList()

        val pinnedSites = appPrefsWrapper.pinnedSiteLocalIds
            .mapNotNull { pinnedId -> allSites.firstOrNull { it.localId == pinnedId } }
            .toMutableList()

        val recentSites = appPrefsWrapper.getRecentSiteLocalIds()
            .mapNotNull { pinnedId -> allSites.firstOrNull { it.localId == pinnedId } }
            .toMutableList()
            .apply { removeAll(pinnedSites) }

        allSites.apply {
            removeAll(pinnedSites)
            removeAll(recentSites)
        }

        return pinnedSites + recentSites + allSites.sortedBy { it.blogNameOrHomeURL }
    }

    /**
     * @return the section name for the site with the given local ID.
     */
    fun getSection(localId: Int): String = when {
        appPrefsWrapper.pinnedSiteLocalIds.contains(localId) -> "pinned"
        appPrefsWrapper.getRecentSiteLocalIds().contains(localId) -> "recent"
        else -> "all"
    }
}
