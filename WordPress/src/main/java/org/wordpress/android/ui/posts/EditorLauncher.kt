package org.wordpress.android.ui.posts

import android.content.Context
import android.content.Intent
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    companion object {
        /**
         * Intent extra key to indicate the Intent was created through EditorLauncher.
         * Used for analytics to distinguish between EditorLauncher and direct Intent creation.
         */
        const val EXTRA_LAUNCHED_VIA_EDITOR_LAUNCHER = "launched_via_editor_launcher"
        /**
         * Static accessor for use in static utility classes like ActivityLauncher.
         * Prefer constructor injection when possible.
         */
        @JvmStatic
        fun getInstance(): EditorLauncher {
            return (getContext().applicationContext as WordPress).component().editorLauncher()
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

        val properties = mapOf(
            "will_use_gutenberg_kit" to shouldUseGutenbergKit
        )
        analyticsTrackerWrapper.track(stat = AnalyticsTracker.Stat.EDITOR_LAUNCHER, properties)

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
     * Adds all editor parameters as Intent extras.
     *
     * Each field in EditorLauncherParams must be handled by one of the add*Extras methods.
     * See EditorLauncherTest for complete field-to-method mapping documentation.
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
        params.isPage?.let { putExtra(EditPostActivityConstants.EXTRA_IS_PAGE, it) }
        params.isPromo?.let { putExtra(EditPostActivityConstants.EXTRA_IS_PROMO, it) }
        putExtra(EXTRA_LAUNCHED_VIA_EDITOR_LAUNCHER, true)
    }

    private fun Intent.addPostExtras(params: EditorLauncherParams) {
        params.postLocalId?.let { putExtra(EditPostActivityConstants.EXTRA_POST_LOCAL_ID, it) }
        params.postRemoteId?.let { putExtra(EditPostActivityConstants.EXTRA_POST_REMOTE_ID, it) }
        params.loadAutoSaveRevision?.let { putExtra(EditPostActivityConstants.EXTRA_LOAD_AUTO_SAVE_REVISION, it) }
        params.isQuickPress?.let { putExtra(EditPostActivityConstants.EXTRA_IS_QUICKPRESS, it) }
        params.isLandingEditor?.let { putExtra(EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR, it) }
        params.isLandingEditorOpenedForNewSite?.let {
            putExtra(EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE, it)
        }
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
}
