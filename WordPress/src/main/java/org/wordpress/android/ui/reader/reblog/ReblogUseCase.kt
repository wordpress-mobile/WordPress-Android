package org.wordpress.android.ui.reader.reblog

import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

@Reusable
class ReblogUseCase @Inject constructor(private val siteStore: SiteStore) {
    fun onReblogButtonClicked(post: ReaderPost): Event<ReblogState> {
        val sites = siteStore.visibleSitesAccessedViaWPCom

        return when (sites.count()) {
            0 -> Event(NoSite)
            1 -> {
                sites.firstOrNull()?.let {
                    Event(PostEditor(it, post))
                } ?: Event(Unknown)
            }
            else -> {
                sites.firstOrNull()?.let {
                    Event(SitePicker(it, post))
                } ?: Event(Unknown)
            }
        }
    }

    fun onReblogSiteSelected(siteLocalId: Int, currentState: ReblogState?): Event<ReblogState> {
        return if (currentState is SitePicker) {
            val site: SiteModel? = siteStore.getSiteByLocalId(siteLocalId)
            if (site != null) Event(PostEditor(site, currentState.post)) else Event(Unknown)
        } else if (BuildConfig.DEBUG) {
            throw IllegalStateException("Site Selected without passing the SitePicker state")
        } else {
            AppLog.e(T.READER, "Site Selected without passing the SitePicker state")
            Event(Unknown)
        }
    }
}
