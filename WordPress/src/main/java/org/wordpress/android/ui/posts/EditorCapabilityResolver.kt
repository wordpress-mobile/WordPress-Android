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
    fun resolveThirdPartyBlocks(site: SiteModel): EditorCapabilityState = when {
        !gutenbergKitFeatureChecker.isGutenbergKitEnabled() -> EditorCapabilityState.Hidden
        !gutenbergKitPluginsFeature.isEnabled() -> EditorCapabilityState.Hidden
        !editorSettingsRepository.getSupportsEditorAssetsForSite(site) ->
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)
        else -> {
            val userEnabled = siteSettingsProvider
                .getSettings(site)
                ?.useThirdPartyBlocks
                ?: DEFAULT_USE_THIRD_PARTY_BLOCKS
            EditorCapabilityState.Available(userEnabled)
        }
    }

    fun resolveThemeStyles(site: SiteModel): EditorCapabilityState = when {
        !gutenbergKitFeatureChecker.isGutenbergKitEnabled() -> EditorCapabilityState.Hidden
        !editorSettingsRepository.getSupportsEditorSettingsForSite(site) ->
            EditorCapabilityState.Unsupported(EditorCapabilityState.UnsupportedReason.CapabilityMissing)
        else -> {
            val userEnabled = siteSettingsProvider
                .getSettings(site)
                ?.useThemeStyles
                ?: DEFAULT_USE_THEME_STYLES
            val advisory = if (!editorSettingsRepository.getThemeSupportsBlockStyles(site)) {
                EditorCapabilityState.AdvisoryReason.ThemeNotBlockTheme
            } else {
                null
            }
            EditorCapabilityState.Available(userEnabled, advisory)
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
sealed class EditorCapabilityState {
    /**
     * Globally disabled (feature flag off). The setting row is
     * hidden; the editor does not apply the capability.
     */
    data object Hidden : EditorCapabilityState()

    /**
     * Globally enabled, but this site cannot use the capability.
     * The setting row is shown but disabled, with a reason the
     * UI can surface to the user.
     */
    data class Unsupported(val reason: UnsupportedReason) : EditorCapabilityState()

    /**
     * Globally enabled and toggle-able for this site.
     * [userEnabled] is the current user preference.
     * [advisory] optionally attaches an informational note — the
     * toggle is still honoured regardless.
     */
    data class Available(
        val userEnabled: Boolean,
        override val advisory: AdvisoryReason? = null,
    ) : EditorCapabilityState()

    enum class UnsupportedReason { CapabilityMissing }
    enum class AdvisoryReason { ThemeNotBlockTheme }

    /**
     * The advisory note attached to this state, or `null` if there
     * is none — including non-[Available] states. Safe for Java
     * callers to query without an instanceof check.
     */
    open val advisory: AdvisoryReason? get() = null

    val shouldApplyInEditor: Boolean
        get() = this is Available && userEnabled

    // Type predicates for Java callers that can't `is`-match
    // on a sealed hierarchy. Kotlin callers should prefer `when`.
    val isHidden: Boolean get() = this is Hidden
    val isUnsupported: Boolean get() = this is Unsupported
    val isAvailable: Boolean get() = this is Available

    val asAvailable: Available? get() = this as? Available
}
