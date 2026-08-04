package org.wordpress.android.ui.newstats

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.NewStatsFeatureConfig
import javax.inject.Inject

/**
 * Single source of truth for deciding whether Stats entry points should open New Stats.
 */
class NewStatsFeatureUtils @Inject constructor(
    private val newStatsFeatureConfig: NewStatsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    /**
     * Returns true when the remote flag is on (rollout) or the user has opted in locally.
     */
    fun isNewStatsEnabled(): Boolean =
        newStatsFeatureConfig.isEnabled() || appPrefsWrapper.getNewStatsUserOptedIn()
}
