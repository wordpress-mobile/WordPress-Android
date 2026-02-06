package org.wordpress.android.ui.posts

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EditorDependencyStore
import org.wordpress.android.util.SiteUtils
import org.wordpress.gutenberg.model.EditorDependencies
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GutenbergEditorPreloader @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val accountStore: AccountStore,
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var lastPreloadedSiteId: Long = -1
    private var preloadJob: Job? = null
    private var cachedDependencies: EditorDependencies? = null

    fun preloadIfNeeded(site: SiteModel, scope: CoroutineScope) {
        if (!gutenbergKitFeatureChecker.isGutenbergKitEnabled()) return
        if (!SiteUtils.isBlockEditorDefaultForNewPost(site)) return
//        if (site.siteId == lastPreloadedSiteId) return
        if (preloadJob?.isActive == true) return

        lastPreloadedSiteId = site.siteId
        preloadJob = scope.launch(bgDispatcher) {
            try {
                val config = GutenbergKitSettingsBuilder
                    .buildPostConfiguration(
                        site = site,
                        accessToken = accountStore.accessToken
                    )
                val store = EditorDependencyStore(appContext, scope)
                cachedDependencies = store.fetch(config)
                AppLog.d(
                    AppLog.T.EDITOR,
                    "Editor dependencies preloaded for site ${site.siteId}"
                )
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.EDITOR,
                    "Failed to preload editor dependencies",
                    e
                )
                cachedDependencies = null
            }
        }
    }

    fun getDependencies(): EditorDependencies? = cachedDependencies

    fun clear() {
        preloadJob?.cancel()
        preloadJob = null
        cachedDependencies = null
        lastPreloadedSiteId = -1
    }
}
