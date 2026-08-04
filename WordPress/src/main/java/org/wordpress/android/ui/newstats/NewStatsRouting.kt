package org.wordpress.android.ui.newstats

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.NewStatsFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for whether New Stats is shown, shared by the My Site menu and the
 * Stats widgets so the two can't drift apart.
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
    }

    fun optOut() {
        appPrefsWrapper.setNewStatsUserOptedOut(true)
        appPrefsWrapper.setNewStatsUserOptedIn(false)
        // Show the intro again if the user later opts back in.
        appPrefsWrapper.setNewStatsIntroShown(false)
    }

    fun hasOptedOut(): Boolean = appPrefsWrapper.getNewStatsUserOptedOut()
}
