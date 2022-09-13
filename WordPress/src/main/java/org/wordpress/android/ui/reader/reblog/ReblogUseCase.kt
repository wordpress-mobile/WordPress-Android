package org.wordpress.android.ui.reader.reblog

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.PagePostCreationSourcesDetail.POST_FROM_REBLOG
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode.REBLOG_SELECT_MODE
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.reblog.ReblogState.MultipleSites
import org.wordpress.android.ui.reader.reblog.ReblogState.NoSite
import org.wordpress.android.ui.reader.reblog.ReblogState.SingleSite
import org.wordpress.android.ui.reader.reblog.ReblogState.Unknown
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfig
import javax.inject.Inject
import javax.inject.Named

@Reusable
class ReblogUseCase @Inject constructor(
    private val siteStore: SiteStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun onReblogButtonClicked(post: ReaderPost): ReblogState {
        return withContext(bgDispatcher) {
            val sites = siteStore.visibleSitesAccessedViaWPCom

            when (sites.count()) {
                0 -> NoSite
                1 -> {
                    sites.firstOrNull()?.let {
                        SingleSite(it, post)
                    } ?: Unknown
                }
                else -> {
                    sites.firstOrNull()?.let {
                        MultipleSites(it, post)
                    } ?: Unknown
                }
            }
        }
    }

    @Suppress("UseCheckOrError")
    suspend fun onReblogSiteSelected(siteLocalId: Int, post: ReaderPost?): ReblogState {
        return withContext(bgDispatcher) {
            when {
                post != null -> {
                    val site: SiteModel? = siteStore.getSiteByLocalId(siteLocalId)
                    if (site != null) SingleSite(site, post) else Unknown
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
    }

    fun convertReblogStateToNavigationEvent(state: ReblogState): ReaderNavigationEvents? {
        return when (state) {
            is NoSite -> ShowNoSitesToReblog
            is MultipleSites -> ShowSitePickerForResult(state.defaultSite, state.post, REBLOG_SELECT_MODE)
            is SingleSite -> OpenEditorForReblog(state.site, state.post, POST_FROM_REBLOG)
            Unknown -> null
        }
    }
}
