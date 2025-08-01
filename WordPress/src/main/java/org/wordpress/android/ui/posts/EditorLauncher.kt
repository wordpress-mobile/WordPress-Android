package org.wordpress.android.ui.posts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized helper for launching editor activities with proper routing logic.
 *
 * This class determines which editor activity to launch based on feature flags
 * and provides analytics tracking for launch methods.
 */
@Singleton
class EditorLauncher @Inject constructor(
    private val gutenbergKitFeature: GutenbergKitFeature,
    private val experimentalFeatures: ExperimentalFeatures,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {

    companion object {
        /**
         * Static accessor for use in static utility classes like ActivityLauncher.
         * Prefer constructor injection when possible.
         */
        @JvmStatic
        fun getInstance(): EditorLauncher {
            return (getContext().applicationContext as org.wordpress.android.WordPress).component().editorLauncher()
        }
    }

    /**
     * Creates an Intent for launching the appropriate editor activity.
     *
     * @param context The context to create the Intent from
     * @param baseIntent Optional intent to copy extras from
     * @return Intent configured for the appropriate editor activity
     */
    fun createEditorIntent(context: Context, baseIntent: Intent? = null): Intent {
        val shouldUseGutenbergKit = shouldUseGutenbergKitEditor()

        // For now, always route to EditPostActivity as scaffold
        // TODO: Route to EditPostGutenbergKitActivity when it exists
        val targetActivity = EditPostActivity::class.java

        val editorType = if (shouldUseGutenbergKit) "gutenberg_kit" else "legacy"
        val source = baseIntent?.let { getIntentSource(it) } ?: "unknown"

        AppLog.d(T.EDITOR, "EditorLauncher: routing to $editorType editor from $source")

        // Track launch method for analytics
        trackEditorLaunch(editorType, source, "helper")

        return Intent(context, targetActivity).apply {
            baseIntent?.let { putExtras(it.extras ?: Bundle()) }
        }
    }

    /**
     * Determines if GutenbergKit editor should be used based on feature flags.
     */
    private fun shouldUseGutenbergKitEditor(): Boolean {
        val isGutenbergEnabled = experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_BLOCK_EDITOR) ||
                gutenbergKitFeature.isEnabled()
        val isGutenbergDisabled = experimentalFeatures.isEnabled(Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR)
        return isGutenbergEnabled && !isGutenbergDisabled
    }

    /**
     * Identifies the source of the editor launch based on intent extras.
     */
    private fun getIntentSource(intent: Intent): String {
        return when {
            intent.hasExtra(EditPostActivityConstants.EXTRA_IS_QUICKPRESS) -> "quickpress"
            intent.hasExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_TITLE) -> "reblog"
            intent.hasExtra(EditPostActivityConstants.EXTRA_INSERT_MEDIA) -> "media_upload"
            intent.action == EditPostActivityConstants.ACTION_REBLOG -> "reblog_action"
            intent.hasExtra(EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR) -> "landing_editor"
            intent.hasExtra(EditPostActivityConstants.EXTRA_IS_PROMO) -> "promo"
            intent.hasExtra(EditPostActivityConstants.EXTRA_IS_PAGE) -> {
                if (intent.getBooleanExtra(EditPostActivityConstants.EXTRA_IS_PAGE, false)) "page" else "post"
            }

            else -> "direct"
        }
    }

    /**
     * Track editor launch events for analytics.
     */
    private fun trackEditorLaunch(editorType: String, source: String, launchMethod: String) {
        val properties = mapOf(
            "editor_type" to editorType,
            "source" to source,
            "launch_method" to launchMethod
        )

        // TODO: Add proper analytics tracking with appropriate event name
        AppLog.i(T.EDITOR, "Editor launch tracked: $properties")
    }
}
