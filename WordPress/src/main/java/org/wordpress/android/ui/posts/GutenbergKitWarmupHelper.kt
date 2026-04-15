package org.wordpress.android.ui.posts

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Helper class to manage GutenbergView warmup for preloading editor assets.
 * This improves editor launch speed by caching WebView assets before the editor is opened.
 */
@Singleton
class GutenbergKitWarmupHelper @Inject constructor(
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var lastWarmedUpSiteId: Long? = null
    private var isWarmupInProgress = false

    /**
     * Triggers warmup for the given site if not already warmed up.
     *
     * @param site The site to warm up the editor for
     * @param scope The coroutine scope to launch the warmup in
     */
    fun warmupIfNeeded(site: SiteModel?, scope: CoroutineScope) {
        when {
            site == null -> {
                AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Skipping warmup - no site provided")
            }
            lastWarmedUpSiteId == site.siteId && !isWarmupInProgress -> {
                AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Already warmed up for site ${site.siteId}")
            }
            isWarmupInProgress -> {
                AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Warmup already in progress")
            }
            !shouldWarmupForSite(site) -> {
                // Logging handled within shouldWarmupForSite()
            }
            else -> {
                scope.launch(bgDispatcher) {
                    performWarmup(site)
                }
            }
        }
    }

    /**
     * Clears the warmup state when switching sites or logging out.
     */
    fun clearWarmupState() {
        lastWarmedUpSiteId = null
        isWarmupInProgress = false
        AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Warmup state cleared")
    }

    private fun shouldWarmupForSite(site: SiteModel): Boolean {
        if (!gutenbergKitFeatureChecker.isGutenbergKitEnabled()) {
            AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Skipping warmup - GutenbergKit features disabled")
            return false
        }

        val shouldWarmup = SiteUtils.isBlockEditorDefaultForNewPost(site)

        if (shouldWarmup) {
            AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Warming site ${site.siteId} " +
                    "(isBlockEditorDefault: true, webEditor: ${site.webEditor})")
        } else {
            AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Skipping warmup - site ${site.siteId} doesn't " +
                    "default to the block editor for new posts " +
                    "(isBlockEditorDefault: false, webEditor: ${site.webEditor})")
        }

        return shouldWarmup
    }

    @Suppress("UnusedParameter")
    private suspend fun performWarmup(site: SiteModel) {
        // GutenbergView.warmup() was removed in GutenbergKit v0.15.0.
        // Warmup/preloading needs to be reimplemented using the new API.
        AppLog.d(
            T.EDITOR,
            "GutenbergKitWarmupHelper: Warmup not yet supported in v0.15.0"
        )
    }
}
