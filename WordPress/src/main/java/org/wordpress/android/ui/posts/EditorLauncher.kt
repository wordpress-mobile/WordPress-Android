package org.wordpress.android.ui.posts

import android.content.Context
import android.content.Intent
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.WordPress
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
    private val experimentalFeatures: ExperimentalFeatures
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
     * @param params Type-safe parameters for editor launch
     * @return Intent configured for the appropriate editor activity
     */
    fun createEditorIntent(context: Context, params: EditorLauncherParams): Intent {
        val shouldUseGutenbergKit = shouldUseGutenbergKitEditor()

        // For now, always route to EditPostActivity as scaffold
        // Will route to EditPostGutenbergKitActivity when it exists
        val targetActivity = EditPostActivity::class.java

        val editorType = if (shouldUseGutenbergKit) "gutenberg_kit" else "legacy"
        val source = getParamsSource(params)

        AppLog.d(T.EDITOR, "EditorLauncher: routing to $editorType editor from $source")

        // Track launch method for analytics
        trackEditorLaunch(editorType, source, "helper")

        return Intent(context, targetActivity).apply {
            addEditorExtras(params)
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
     * Identifies the source of the editor launch based on EditorLauncherParams.
     */
    private fun getParamsSource(params: EditorLauncherParams): String {
        return when {
            params.isQuickPress -> "quickpress"
            params.reblogPostTitle != null -> "reblog"
            params.insertMedia != null -> "media_upload"
            params.reblogAction != null -> "reblog_action"
            params.isLandingEditor -> "landing_editor"
            params.isPromo -> "promo"
            params.isPage -> "page"
            params.postLocalId != null || params.postRemoteId != null -> "edit_post"
            params.source != null -> params.source.toString().lowercase()
            else -> "new_post"
        }
    }


    /**
     * Adds all editor parameters as Intent extras.
     */
    private fun Intent.addEditorExtras(params: EditorLauncherParams) {
        addBasicExtras(params)
        addPostExtras(params)
        addReblogExtras(params)
        addPageExtras(params)
        addMiscExtras(params)
    }

    private fun Intent.addBasicExtras(params: EditorLauncherParams) {
        putExtra(WordPress.SITE, params.site)
        putExtra(EditPostActivityConstants.EXTRA_IS_PAGE, params.isPage)
        putExtra(EditPostActivityConstants.EXTRA_IS_PROMO, params.isPromo)
    }

    private fun Intent.addPostExtras(params: EditorLauncherParams) {
        params.postLocalId?.let { putExtra(EditPostActivityConstants.EXTRA_POST_LOCAL_ID, it) }
        params.postRemoteId?.let { putExtra(EditPostActivityConstants.EXTRA_POST_REMOTE_ID, it) }
        putExtra(EditPostActivityConstants.EXTRA_LOAD_AUTO_SAVE_REVISION, params.loadAutoSaveRevision)
        putExtra(EditPostActivityConstants.EXTRA_IS_QUICKPRESS, params.isQuickPress)
        putExtra(EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR, params.isLandingEditor)
        putExtra(
            EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE,
            params.isLandingEditorOpenedForNewSite
        )
    }

    private fun Intent.addReblogExtras(params: EditorLauncherParams) {
        params.reblogPostTitle?.let { putExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_TITLE, it) }
        params.reblogPostQuote?.let { putExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_QUOTE, it) }
        params.reblogPostImage?.let { putExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_IMAGE, it) }
        params.reblogPostCitation?.let { putExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_CITATION, it) }
        params.reblogAction?.let { action = it }
    }

    private fun Intent.addPageExtras(params: EditorLauncherParams) {
        params.pageTitle?.let { putExtra(EditPostActivityConstants.EXTRA_PAGE_TITLE, it) }
        params.pageContent?.let { putExtra(EditPostActivityConstants.EXTRA_PAGE_CONTENT, it) }
        params.pageTemplate?.let { putExtra(EditPostActivityConstants.EXTRA_PAGE_TEMPLATE, it) }
    }

    private fun Intent.addMiscExtras(params: EditorLauncherParams) {
        params.voiceContent?.let { putExtra(EditPostActivityConstants.EXTRA_VOICE_CONTENT, it) }
        params.insertMedia?.let { putExtra(EditPostActivityConstants.EXTRA_INSERT_MEDIA, it) }
        params.source?.let { putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, it) }
        params.promptId?.let { putExtra(EditPostActivityConstants.EXTRA_PROMPT_ID, it) }
        params.entryPoint?.let { putExtra(EditPostActivityConstants.EXTRA_ENTRY_POINT, it) }
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

        // Analytics tracking will be implemented with appropriate event name
        AppLog.i(T.EDITOR, "Editor launch tracked: $properties")
    }
}
