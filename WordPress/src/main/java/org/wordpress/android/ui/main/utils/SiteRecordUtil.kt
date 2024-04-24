package org.wordpress.android.ui.main.utils

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.SiteRecord
import java.util.Locale

object SiteRecordUtil {
    /**
     * Sorts the list of sites by blog name or home URL and moves the primary site to the top.
     */
    @JvmStatic
    fun sort(sites: List<SiteRecord>, primarySiteId: Long): List<SiteRecord> {
        val list = sites.sortedBy { it.blogNameOrHomeURL }.toMutableList()
        val primarySite = list.firstOrNull { it.siteId == primarySiteId }
        list.remove(primarySite)
        return if (primarySite == null) list else (listOf(primarySite) + list)
    }

    @JvmStatic
    fun sort(sites: List<SiteRecord>, pinnedSiteLocalIds:Set<Int>): List<SiteRecord> {
        val sortedSites = sites.sortedBy { it.blogNameOrHomeURL }
            .toMutableList()

        val pinnedSites = sites.filter { pinnedSiteLocalIds.contains(it.localId) }
        pinnedSites.forEach { sortedSites.remove(it) }

        return pinnedSites + sortedSites
    }

    /**
     * Returns the index of the site with the given local ID.
     */
    @JvmStatic
    fun indexOf(sites: List<SiteRecord>, localId: Int) = sites.indexOfFirst { it.localId == localId }

    /**
     * Returns the index of the given site.
     */
    @JvmStatic
    fun indexOf(sites: List<SiteRecord>, siteRecord: SiteRecord) = sites.indexOfFirst { it.siteId == siteRecord.siteId }

    @Suppress("ReturnCount")
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
