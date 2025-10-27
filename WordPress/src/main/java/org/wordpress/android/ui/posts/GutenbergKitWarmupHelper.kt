package org.wordpress.android.ui.posts

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.config.GutenbergKitPluginsFeature
import org.wordpress.gutenberg.EditorConfiguration
import org.wordpress.gutenberg.GutenbergView
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Helper class to manage GutenbergView warmup for preloading editor assets.
 * This improves editor launch speed by caching WebView assets before the editor is opened.
 */
@Singleton
class GutenbergKitWarmupHelper @Inject constructor(
    private val appContext: Context,
    private val accountStore: AccountStore,
    private val userAgent: UserAgent,
    private val perAppLocaleManager: PerAppLocaleManager,
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    private val gutenbergKitPluginsFeature: GutenbergKitPluginsFeature,
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

    private suspend fun performWarmup(site: SiteModel) {
        try {
            isWarmupInProgress = true
            AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Starting warmup for site ${site.siteId}")

            val configuration = buildWarmupConfiguration(site)

            // Perform the warmup on the main thread as it involves WebView
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                GutenbergView.warmup(appContext, configuration)
            }

            lastWarmedUpSiteId = site.siteId
            AppLog.d(T.EDITOR, "GutenbergKitWarmupHelper: Warmup completed for site ${site.siteId}")
        } catch (e: IllegalStateException) {
            AppLog.e(T.EDITOR, "GutenbergKitWarmupHelper: Warmup failed - illegal state", e)
        } finally {
            isWarmupInProgress = false
        }
    }

    private fun buildWarmupConfiguration(site: SiteModel): EditorConfiguration {
        // Build the configuration using the same patterns as GutenbergKitSettingsBuilder
        val siteConfig = GutenbergKitSettingsBuilder.SiteConfig.fromSiteModel(site)

        // Create minimal post config for warmup (no specific post data)
        val postConfig = GutenbergKitSettingsBuilder.PostConfig(
            remotePostId = null,
            isPage = false,
            title = "",
            content = ""
        )

        val appConfig = GutenbergKitSettingsBuilder.AppConfig(
            accessToken = accountStore.accessToken,
            locale = perAppLocaleManager.getCurrentLocaleLanguageCode(),
            cookies = null, // No cookies needed for warmup
            accountUserId = accountStore.account.userId,
            accountUserName = accountStore.account.userName,
            userAgent = userAgent,
            isJetpackSsoEnabled = false // Default to false for warmup
        )

        val featureConfig = GutenbergKitSettingsBuilder.FeatureConfig(
            isPluginsFeatureEnabled = gutenbergKitPluginsFeature.isEnabled(),
            // Default to true during warmup; actual value will be used when editor launches
            isThemeStylesFeatureEnabled = true
        )

        val settings = GutenbergKitSettingsBuilder.buildSettings(
            siteConfig = siteConfig,
            postConfig = postConfig,
            appConfig = appConfig,
            featureConfig = featureConfig
        )

        return EditorConfigurationBuilder.build(settings, editorSettings = null)
    }
}
