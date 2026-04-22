package org.wordpress.android.ui.posts

import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.util.config.GutenbergKitPluginsFeature
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for whether a given editor capability
 * applies to a site. Combines:
 *
 * 1. A global feature-flag gate (the top-level GutenbergKit flag,
 *    plus any capability-specific remote flag such as
 *    [GutenbergKitPluginsFeature]).
 * 2. A site-level capability cache populated by
 *    [EditorSettingsRepository.fetchEditorCapabilitiesForSite].
 * 3. The user's per-site toggle stored in
 *    [SiteSettingsProvider].
 *
 * Both the settings UI ([SiteSettingsFragment]) and the editor
 * configuration builder consult this resolver so they cannot
 * drift out of agreement.
 */
@Singleton
class EditorCapabilityResolver @Inject constructor(
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    private val gutenbergKitPluginsFeature: GutenbergKitPluginsFeature,
    private val editorSettingsRepository: EditorSettingsRepository,
    private val siteSettingsProvider: SiteSettingsProvider,
) {
    fun resolveThirdPartyBlocks(site: SiteModel): Resolved = when {
        !gutenbergKitFeatureChecker.isGutenbergKitEnabled() -> Resolved.Hidden
        !gutenbergKitPluginsFeature.isEnabled() -> Resolved.Hidden
        !editorSettingsRepository.getSupportsEditorAssetsForSite(site) ->
            Resolved.Unsupported(Resolved.UnsupportedReason.CapabilityMissing)
        else -> {
            val userEnabled = siteSettingsProvider
                .getSettings(site)
                ?.useThirdPartyBlocks
                ?: DEFAULT_USE_THIRD_PARTY_BLOCKS
            Resolved.Available(userEnabled)
        }
    }

    fun resolveThemeStyles(site: SiteModel): Resolved = when {
        !gutenbergKitFeatureChecker.isGutenbergKitEnabled() -> Resolved.Hidden
        !editorSettingsRepository.getSupportsEditorSettingsForSite(site) ->
            Resolved.Unsupported(Resolved.UnsupportedReason.CapabilityMissing)
        else -> {
            val userEnabled = siteSettingsProvider
                .getSettings(site)
                ?.useThemeStyles
                ?: DEFAULT_USE_THEME_STYLES
            val advisory = if (!editorSettingsRepository.getThemeSupportsBlockStyles(site)) {
                Resolved.AdvisoryReason.ThemeNotBlockTheme
            } else {
                null
            }
            Resolved.Available(userEnabled, advisory)
        }
    }

    companion object {
        private const val DEFAULT_USE_THIRD_PARTY_BLOCKS = false
        private const val DEFAULT_USE_THEME_STYLES = true
    }
}

/**
 * Resolved state for an editor capability for a specific site.
 *
 * [shouldApplyInEditor] collapses the state to a single boolean
 * for editor-config callers; the UI layer branches on the full
 * sealed hierarchy to pick the correct visibility / disabled /
 * advisory-note treatment.
 */
sealed class Resolved {
    /**
     * Globally disabled (feature flag off). The setting row is
     * hidden; the editor does not apply the capability.
     */
    data object Hidden : Resolved()

    /**
     * Globally enabled, but this site cannot use the capability.
     * The setting row is shown but disabled, with a reason the
     * UI can surface to the user.
     */
    data class Unsupported(val reason: UnsupportedReason) : Resolved()

    /**
     * Globally enabled and toggle-able for this site.
     * [userEnabled] is the current user preference.
     * [advisory] optionally attaches an informational note — the
     * toggle is still honoured regardless.
     */
    data class Available(
        val userEnabled: Boolean,
        val advisory: AdvisoryReason? = null,
    ) : Resolved()

    enum class UnsupportedReason { CapabilityMissing }
    enum class AdvisoryReason { ThemeNotBlockTheme }

    // Accessors for Java callers that can't `is`-match on a
    // sealed hierarchy. Kotlin callers should prefer `when`.
    val isHidden: Boolean get() = this is Hidden
    val isUnsupported: Boolean get() = this is Unsupported
    val isAvailable: Boolean get() = this is Available

    val asAvailable: Available? get() = this as? Available

    val shouldApplyInEditor: Boolean
        get() = this is Available && userEnabled
}
