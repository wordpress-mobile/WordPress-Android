package org.wordpress.android.ui.posts

import android.content.Context
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
import org.wordpress.android.util.AppLog
import org.wordpress.gutenberg.model.EditorDependencies
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Opportunistically preloads GutenbergKit editor dependencies in the
 * background so the editor opens faster.
 *
 * Cached dependencies are keyed by site local ID, so switching
 * between sites does not discard previously preloaded results.
 *
 * ## Usage
 *
 * - [preloadIfNeeded] — idempotent; call whenever a site becomes
 *   visible. Skips work if the site was already preloaded or a job
 *   is in flight.
 * - [refreshPreloading] — discards the cached result for a site
 *   and re-preloads from scratch (e.g. on pull-to-refresh).
 * - [getDependencies] — returns the cached result for a site, or
 *   `null` if preloading has not completed. Callers must handle
 *   `null` gracefully by loading dependencies themselves.
 * - [clear] — cancels all in-flight work and releases all cached
 *   data. Call when the driving scope is being destroyed.
 *
 * ## Threading
 *
 * Public methods are annotated [@MainThread] and must only be
 * called from the main thread. [state] is a [ConcurrentHashMap],
 * so the background coroutine can safely write [Ready] or remove
 * entries without thread-hopping.
 *
 * ## Deduplication
 *
 * Preloading is skipped when the site already has a cached result
 * or an in-flight job. On failure the entry is removed so the
 * next visit retries automatically. If a caller's coroutine scope
 * is cancelled externally, [shouldPreload] detects the dead
 * [Loading] entry and allows a fresh attempt.
 */
@Singleton
class GutenbergEditorPreloader @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val accountStore: AccountStore,
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    private val gutenbergKitSettingsBuilder: GutenbergKitSettingsBuilder,
    private val siteSettingsProvider: SiteSettingsProvider,
    private val editorServiceProvider: EditorServiceProvider,
    private val editorSettingsRepository: EditorSettingsRepository,
    private val siteApiRestUrlRecoverer: SiteApiRestUrlRecoverer,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private sealed class PreloadState {
        data class Loading(val job: Job) : PreloadState()
        data class Ready(
            val dependencies: EditorDependencies
        ) : PreloadState()
    }

    // Keyed by SiteModel.id (the local DB row ID — stable across the
    // process lifetime, unlike the remote siteId which is 0 for
    // unauthenticated self-hosted sites until discovery completes).
    private val state = ConcurrentHashMap<Int, PreloadState>()

    /**
     * Starts a background preload for [site] if one hasn't already
     * been performed for this site and no job is currently in
     * flight for it.
     *
     * [scope] is the caller's [CoroutineScope] (typically
     * `viewModelScope`); the launched coroutine is cancelled when
     * that scope is cancelled.
     */
    @MainThread
    fun preloadIfNeeded(site: SiteModel, scope: CoroutineScope) {
        if (!shouldPreload(site)) return

        val siteId = site.id
        val job = scope.launch(bgDispatcher) {
            try {
                if (site.wpApiRestUrl.isNullOrEmpty()) {
                    siteApiRestUrlRecoverer.discoverApiRootUrl(site.url)
                        ?.let { site.wpApiRestUrl = it }
                }
                editorSettingsRepository
                    .fetchEditorCapabilitiesForSite(site)
                // Preloading produces EditorDependencies, which the editor
                // consumes alongside its own per-launch EditorConfiguration.
                // Locale, cookies, and network-logging are per-launch
                // concerns the preloaded dependencies don't depend on, so
                // pass safe defaults here.
                val config = gutenbergKitSettingsBuilder
                    .buildPostConfiguration(
                        site = site,
                        accessToken = accountStore.accessToken,
                        locale = "en",
                        cookies = emptyMap(),
                        isNetworkLoggingEnabled = false,
                    )
                val result = editorServiceProvider.prepare(
                    context = appContext,
                    configuration = config,
                    coroutineScope = scope
                )
                state[siteId] = PreloadState.Ready(result)
                AppLog.d(
                    AppLog.T.EDITOR,
                    "Editor dependencies preloaded for" +
                        " site ${site.name}"
                )
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                AppLog.e(
                    AppLog.T.EDITOR,
                    "Failed to preload editor dependencies",
                    e
                )
                state.remove(siteId)
            }
        }
        state[siteId] = PreloadState.Loading(job)
    }

    /**
     * Discards any cached result for [site] and re-preloads from
     * scratch. Use for pull-to-refresh or any scenario where the
     * caller wants to force a fresh fetch.
     */
    @MainThread
    fun refreshPreloading(site: SiteModel, scope: CoroutineScope) {
        clearSite(site)
        preloadIfNeeded(site, scope)
    }

    /**
     * Returns the preloaded dependencies for [site], or `null` if
     * preloading has not completed (or failed). Callers must handle
     * `null` gracefully by loading dependencies themselves.
     */
    @MainThread
    fun getDependencies(site: SiteModel): EditorDependencies? =
        getDependencies(site.id)

    @MainThread
    fun getDependencies(siteLocalId: Int): EditorDependencies? =
        (state[siteLocalId] as? PreloadState.Ready)?.dependencies

    /**
     * Cancels all in-flight preloads and discards all cached
     * results. Call when the driving scope is being destroyed.
     */
    @MainThread
    fun clear() {
        state.values.forEach { entry ->
            if (entry is PreloadState.Loading) entry.job.cancel()
        }
        state.clear()
    }

    private fun clearSite(site: SiteModel) {
        val entry = state.remove(site.id)
        if (entry is PreloadState.Loading) entry.job.cancel()
    }

    private fun shouldPreload(site: SiteModel): Boolean {
        val isEnabled =
            gutenbergKitFeatureChecker.isGutenbergKitEnabled() &&
                siteSettingsProvider.isBlockEditorDefault(site)
        val isAlreadyHandled = when (val entry = state[site.id]) {
            is PreloadState.Loading -> entry.job.isActive
            is PreloadState.Ready -> true
            null -> false
        }
        return isEnabled && !isAlreadyHandled
    }
}
