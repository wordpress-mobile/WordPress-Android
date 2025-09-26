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
     * Data class containing the state of all GutenbergKit-related feature flags.
     */
    data class FeatureState(
        val isExperimentalBlockEditorEnabled: Boolean,
        val isGutenbergKitFeatureEnabled: Boolean,
        val isDisableExperimentalBlockEditorEnabled: Boolean
    ) {
        /**
         * Determines if GutenbergKit should be enabled based on the feature states.
         */
        val isGutenbergKitEnabled: Boolean
            get() = (isExperimentalBlockEditorEnabled || isGutenbergKitFeatureEnabled) &&
                    !isDisableExperimentalBlockEditorEnabled
    }

    /**
     * Gets the current state of all GutenbergKit-related feature flags.
     *
     * @return FeatureState containing all flag states and the computed enabled state
     */
    fun getFeatureState(): FeatureState {
        return FeatureState(
            isExperimentalBlockEditorEnabled = experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR),
            isGutenbergKitFeatureEnabled = gutenbergKitFeature.isEnabled(),
            isDisableExperimentalBlockEditorEnabled = experimentalFeatures.isEnabled(
                Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR
            )
        )
    }

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
        return getFeatureState().isGutenbergKitEnabled
    }
}
