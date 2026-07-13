package org.wordpress.android.ui.posts.editor

import androidx.core.util.Consumer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.XPostsResult
import org.wordpress.android.fluxc.store.XPostsSource
import org.wordpress.android.fluxc.store.XPostsStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class XPostsCapabilityChecker @Inject constructor(
    private val xPostsStore: XPostsStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val currentTimeProvider: CurrentTimeProvider
) {
    @Suppress("GlobalCoroutineUsage")
    @OptIn(DelicateCoroutinesApi::class)
    fun retrieveCapability(site: SiteModel, callback: Consumer<Boolean>) {
        GlobalScope.launch(Dispatchers.IO) {
            val capability = isCapable(site)
            callback.accept(capability)
        }
    }

    suspend fun isCapable(site: SiteModel): Boolean =
        // Check db first in order to reduce unnecessary network calls
        when (val saved = xPostsStore.getXPostsFromDb(site)) {
            // A non-empty persisted list means the site has xposts. We keep trusting this even if the
            // site later loses them, to avoid an expensive fetch on every editor open.
            is XPostsResult.Result -> if (saved.xPosts.isNotEmpty()) true else recheckIfStale(site)
            // We have never gotten a response for this site, so make the api call to try to find out.
            is XPostsResult.Unknown -> recheckIfStale(site)
        }

    /**
     * Called when the cache says the site has no xposts (or we have never checked). We avoid re-fetching
     * on every editor open — for non-P2 sites that request is rejected with a recurring
     * `xposts_require_o2_enabled` (HTTP 400). But a site can gain xposts later (e.g. o2 gets enabled), so
     * we re-query once the last confirmed "no xposts" result is older than [NO_XPOSTS_TTL_MS].
     */
    private suspend fun recheckIfStale(site: SiteModel): Boolean {
        val lastChecked = appPrefsWrapper.getXPostsNoResultCheckedTimestamp(site)
        val now = currentTimeProvider.currentDate().time
        // The `now >= lastChecked` guard treats a timestamp from the future (device clock moved
        // backwards) as stale, so a site can't get stuck as not-capable until the clock catches up.
        if (lastChecked != 0L && now >= lastChecked && now - lastChecked < NO_XPOSTS_TTL_MS) {
            return false
        }
        return when (val fetched = xPostsStore.fetchXPosts(site)) {
            is XPostsResult.Result -> {
                val hasXposts = fetched.xPosts.isNotEmpty()
                // Only throttle when the server actually confirmed there are no xposts. An empty result
                // sourced from the db means the network call failed and fell back to the cache; treating
                // that as a confirmation would delay picking up xposts a site enabled in the meantime.
                if (!hasXposts && fetched.source == XPostsSource.REST_API) {
                    appPrefsWrapper.setXPostsNoResultCheckedTimestamp(site, now)
                }
                hasXposts
            }
            // We don't know whether the site has any xposts (e.g. a network error with no cached data),
            // so default to capable and record nothing, so that we re-check next time.
            is XPostsResult.Unknown -> true
        }
    }

    companion object {
        private val NO_XPOSTS_TTL_MS = TimeUnit.DAYS.toMillis(1)
    }
}
