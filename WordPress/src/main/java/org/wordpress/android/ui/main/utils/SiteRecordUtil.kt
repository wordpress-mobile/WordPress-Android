package org.wordpress.android.ui.main.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord
import java.util.Locale


object SiteRecordUtil {
    fun sortByName(sites: List<SiteRecord>) = sites.sortedBy { it.blogNameOrHomeURL }

    @JvmStatic
    fun indexOf(sites: List<SiteRecord>, localId: Int) = sites.indexOfFirst { it.localId == localId }

    @JvmStatic
    fun indexOf(sites: List<SiteRecord>, siteRecord: SiteRecord) = sites.indexOfFirst { it.siteId == siteRecord.siteId }

    @JvmStatic
    fun isSameList(currentSites: List<SiteRecord>, anotherSites: List<SiteRecord>): Boolean {
        if (currentSites.size != anotherSites.size) {
            return false
        }

        for (i in currentSites.indices) {
            if (currentSites[i].siteId != anotherSites[i].siteId ||
                currentSites[i].isHidden != anotherSites[i].isHidden ||
                currentSites[i].isRecentPick != anotherSites[i].isRecentPick
            ) return false
        }
        return true
    }

    @JvmStatic
    fun filteredSites(sites: List<SiteRecord>, searchKeyword: String) =
        sites.filter { record ->
            val siteName: String = record.blogName.lowercase(Locale.getDefault())
            val hostName: String = record.homeURL.lowercase(Locale.ROOT)

            siteName.contains(searchKeyword.lowercase(Locale.getDefault())) ||
                    hostName.contains(searchKeyword.lowercase(Locale.ROOT))
        }

    @JvmStatic
    fun createRecords(siteModels: List<SiteModel>) = siteModels.map { SiteRecord(it) }
}

