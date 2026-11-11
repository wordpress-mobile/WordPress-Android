package org.wordpress.android.ui.posts

import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the decisions for the GutenbergKit announcement bottom sheet and the per-site override
 * it writes. Pure logic so it is unit-testable; the fragment and Site Settings only call into it.
 *
 * The per-site override is the single source of truth — its presence means the user has decided
 * for that site (either direction), its absence means "not yet decided." "Maybe later" defers the
 * announcement for one week per-site rather than writing an override, so we don't mis-read the
 * user's intent.
 */
@Singleton
class GutenbergKitAnnouncementController @Inject constructor(
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    private val siteSettingsProvider: SiteSettingsProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val clock: Clock,
) {
    @Suppress("ReturnCount")
    fun shouldShowAnnouncement(site: SiteModel): Boolean {
        // The per-site override/deferral prefs are keyed by URL, so a site without one would
        // loop the announcement on every resume (writes would no-op via TextUtils.isEmpty).
        if (site.url.isNullOrEmpty()) return false
        if (!gutenbergKitFeatureChecker.isGutenbergKitRemoteFeatureEnabled()) return false
        if (!siteSettingsProvider.isBlockEditorDefault(site)) return false
        if (appPrefsWrapper.getGutenbergKitSiteOverride(site.url) != null) return false
        return clock.millis() >= appPrefsWrapper.getGutenbergKitAnnouncementDeferredUntil(site.url)
    }

    fun onActivate(site: SiteModel) = setOverride(site, true)

    /**
     * Records an explicit per-site decision (from the announcement sheet or Site Settings). An
     * explicit decision supersedes any pending "Maybe later" deferral on the same site, so this
     * clears the deferral timestamp as well.
     */
    fun setOverride(site: SiteModel, enabled: Boolean) {
        appPrefsWrapper.setGutenbergKitSiteOverride(site.url, enabled)
        appPrefsWrapper.setGutenbergKitAnnouncementDeferredUntil(site.url, 0L)
    }

    fun onMaybeLater(site: SiteModel) {
        appPrefsWrapper.setGutenbergKitAnnouncementDeferredUntil(
            site.url,
            clock.millis() + DEFER_DURATION_MILLIS
        )
    }

    companion object {
        val DEFER_DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(7)
    }
}
