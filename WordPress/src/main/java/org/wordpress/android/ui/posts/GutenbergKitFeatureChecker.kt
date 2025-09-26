package org.wordpress.android.ui.posts

import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized utility for checking if GutenbergKit feature is enabled.
 * This consolidates the logic that was previously duplicated across multiple classes.
 */
@Singleton
class GutenbergKitFeatureChecker @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val gutenbergKitFeature: GutenbergKitFeature
) {
    /**
     * Determines if GutenbergKit is enabled based on feature flags.
     *
     * The feature is enabled if:
     * - Either the experimental block editor is enabled OR the GutenbergKit feature flag is on
     * - AND the disable experimental block editor flag is NOT enabled
     *
     * @return true if GutenbergKit should be enabled, false otherwise
     */
    fun isGutenbergKitEnabled(): Boolean {
        val isGutenbergEnabled = experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR) ||
                gutenbergKitFeature.isEnabled()
        val isGutenbergDisabled = experimentalFeatures.isEnabled(Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR)
        return isGutenbergEnabled && !isGutenbergDisabled
    }
}