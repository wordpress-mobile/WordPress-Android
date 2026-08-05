package org.wordpress.android.ui.newstats

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.NewStatsFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether New Stats is shown, shared by the My Site menu and the Stats widgets so the two
 * can't drift apart.
 *
 * Note this does not yet govern every route into Stats: shortcuts, deep links, notifications and
 * the activity log go straight to the old [org.wordpress.android.ui.stats.refresh.StatsActivity]
 * via ActivityLauncher.viewBlogStats, regardless of this decision.
 */
@Singleton
class NewStatsRouting @Inject constructor(
    private val newStatsFeatureConfig: NewStatsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    /**
     * New Stats is the default when the remote flag is on (rollout) or the user opted in locally,
     * but an explicit opt-out always wins so users on the rollout can switch back.
     */
    fun isNewStatsEnabled(): Boolean =
        !appPrefsWrapper.getNewStatsUserOptedOut() &&
                (newStatsFeatureConfig.isEnabled() || appPrefsWrapper.getNewStatsUserOptedIn())

    fun optIn() {
        appPrefsWrapper.setNewStatsUserOptedOut(false)
        appPrefsWrapper.setNewStatsUserOptedIn(true)
        // Reintroduce New Stats on the way in. Resetting this on opt-out instead would re-arm the
        // intro sheet while NewStatsActivity is still on screen, so it reappears on recreation.
        appPrefsWrapper.setNewStatsIntroShown(false)
    }

    fun optOut() {
        appPrefsWrapper.setNewStatsUserOptedOut(true)
        appPrefsWrapper.setNewStatsUserOptedIn(false)
    }

    fun hasOptedOut(): Boolean = appPrefsWrapper.getNewStatsUserOptedOut()
}
