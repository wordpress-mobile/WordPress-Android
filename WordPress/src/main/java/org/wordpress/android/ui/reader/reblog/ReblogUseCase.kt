package org.wordpress.android.ui.reader.reblog

import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_REBLOG
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode.REBLOG_SELECT_MODE
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfig
import javax.inject.Inject

@Reusable
class ReblogUseCase @Inject constructor(private val siteStore: SiteStore) {
    fun onReblogButtonClicked(post: ReaderPost): ReblogState {
        val sites = siteStore.visibleSitesAccessedViaWPCom

        return when (sites.count()) {
            0 -> NoSite
            1 -> {
                sites.firstOrNull()?.let {
                    PostEditor(it, post)
                } ?: Unknown
            }
            else -> {
                sites.firstOrNull()?.let {
                    SitePicker(it, post)
                } ?: Unknown
            }
        }
    }

    fun onReblogSiteSelected(siteLocalId: Int, post: ReaderPost?): ReblogState {
        return when {
            post != null -> {
                val site: SiteModel? = siteStore.getSiteByLocalId(siteLocalId)
                if (site != null) PostEditor(site, post) else Unknown
            }
            BuildConfig.DEBUG -> {
                throw IllegalStateException("Site Selected without passing the SitePicker state")
            }
            else -> {
                AppLog.e(T.READER, "Site Selected without passing the SitePicker state")
                Unknown
            }
        }
    }

    fun convertReblogStateToNavigationEvent(state: ReblogState): ReaderNavigationEvents? {
        return when (state) {
            is NoSite -> ShowNoSitesToReblog
            is SitePicker -> ShowSitePickerForResult(state.site, state.post, REBLOG_SELECT_MODE)
            is PostEditor -> OpenEditorForReblog(state.site, state.post, POST_FROM_REBLOG)
            Unknown -> null
        }
    }
}
