package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
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
    private val gutenbergKitFeature: GutenbergKitFeature,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    /**
     * Data class containing the state of all GutenbergKit-related feature flags.
     */
    data class FeatureState(
        val isExperimentalBlockEditorEnabled: Boolean,
        val isGutenbergKitFeatureEnabled: Boolean,
        val siteOverride: Boolean? = null
    ) {
        /**
         * Determines if GutenbergKit should be used for editor routing.
         *
         * Resolution: a per-site override (set or cleared via the announcement sheet or Site
         * Settings) wins. When absent, falls back to the experimental flag.
         *
         * The remote `gutenberg_kit` feature flag is deliberately NOT part of editor routing
         * — it only gates the visibility of opt-in surfaces (the announcement bottom sheet and
         * the Site Settings toggle). This lets us roll out the announcement to a percentage of
         * users without simultaneously flipping the default editor. When we're ready to make
         * GutenbergKit the default for everyone, the change is a one-line edit here.
         */
        val isGutenbergKitEnabled: Boolean
            get() = siteOverride ?: isExperimentalBlockEditorEnabled
    }

    /**
     * Gets the current state of all GutenbergKit-related feature flags for the given site (if any).
     */
    @JvmOverloads
    fun getFeatureState(site: SiteModel? = null): FeatureState {
        return FeatureState(
            isExperimentalBlockEditorEnabled = experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR),
            isGutenbergKitFeatureEnabled = gutenbergKitFeature.isEnabled(),
            siteOverride = site?.url?.let { appPrefsWrapper.getGutenbergKitSiteOverride(it) }
        )
    }

    /**
     * Determines if GutenbergKit is enabled based on feature flags (and optional per-site opt-in).
     */
    @JvmOverloads
    fun isGutenbergKitEnabled(site: SiteModel? = null): Boolean {
        return getFeatureState(site).isGutenbergKitEnabled
    }

    /**
     * Whether the user-facing remote feature flag is on (controls opt-in surfaces).
     */
    fun isGutenbergKitRemoteFeatureEnabled(): Boolean = gutenbergKitFeature.isEnabled()
}
