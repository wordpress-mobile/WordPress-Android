package org.wordpress.android.ui.posts

import android.content.Context
import android.content.Intent
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.posts.EditorConstants.RestartEditorOptions
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
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
    private val gutenbergKitFeatureChecker: GutenbergKitFeatureChecker,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteStore: SiteStore,
    private val postStore: PostStore
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

        /**
         * Checks if the editor should restart based on intent extras.
         * Moved from EditPostActivity companion object to make it shareable
         * between editor activities and centralize launch-related logic.
         */
        @JvmStatic
        fun checkToRestart(data: Intent): Boolean {
            val extraRestartEditor = data.getStringExtra(EditorConstants.EXTRA_RESTART_EDITOR)
            return extraRestartEditor != null
                    && RestartEditorOptions.valueOf(extraRestartEditor) != RestartEditorOptions.NO_RESTART
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
        val shouldUseGutenbergKit = shouldUseGutenbergKitEditor(params)

        val targetActivity = if (shouldUseGutenbergKit) {
            GutenbergKitActivity::class.java
        } else {
            EditPostActivity::class.java
        }

        val properties = mapOf(
            "will_use_gutenberg_kit" to shouldUseGutenbergKit
        )
        analyticsTrackerWrapper.track(stat = AnalyticsTracker.Stat.EDITOR_LAUNCHER, properties)

        return Intent(context, targetActivity).apply {
            addEditorExtras(params)
        }
    }

    /**
     * Determines if GutenbergKit editor should be used based on feature flags and post content.
     */
    private fun shouldUseGutenbergKitEditor(params: EditorLauncherParams): Boolean {
        val featureState = gutenbergKitFeatureChecker.getFeatureState()
        val isGutenbergFeatureEnabled = featureState.isGutenbergKitEnabled

        val site = params.siteSource.getSite(siteStore)
        return when {
            !isGutenbergFeatureEnabled -> {
                logFeatureDisabledReason(featureState)
                false
            }

            site == null -> {
                logNoSiteInfoReason()
                true
            }

            else -> {
                determineEditorForSite(params, site)
            }
        }
    }

    private fun determineEditorForSite(params: EditorLauncherParams, site: SiteModel): Boolean {
        val post = getPostFromParams(params)
        val isNewPost = post == null || post.isLocalDraft
        val postContent = post?.content ?: ""
        val shouldUseGutenberg = PostUtils.shouldShowGutenbergEditor(isNewPost, postContent, site)

        logEditorDecision(shouldUseGutenberg, isNewPost, postContent, post, site)
        return shouldUseGutenberg
    }

    private fun logFeatureDisabledReason(featureState: GutenbergKitFeatureChecker.FeatureState) {
        val reason = when {
            featureState.isDisableExperimentalBlockEditorEnabled ->
                "the experimental block editor is explicitly disabled"
            !featureState.isExperimentalBlockEditorEnabled && !featureState.isGutenbergKitFeatureEnabled ->
                "neither the experimental block editor feature nor GutenbergKit feature is enabled"
            else -> "GutenbergKit feature checks failed"
        }
        val featureFlags = getFeatureFlagsString(featureState)
        AppLog.d(AppLog.T.EDITOR, "GutenbergKit editor is NOT being used because $reason $featureFlags")
    }

    private fun logNoSiteInfoReason() {
        val featureFlags = getFeatureFlagsString()
        AppLog.d(
            AppLog.T.EDITOR, "GutenbergKit editor is being used because no site information " +
                    "is available, defaulting to GutenbergKit $featureFlags"
        )
    }

    private fun getFeatureFlagsString(featureState: GutenbergKitFeatureChecker.FeatureState): String {
        return "(experimental_block_editor: ${featureState.isExperimentalBlockEditorEnabled}, " +
                "gutenberg_kit_feature: ${featureState.isGutenbergKitFeatureEnabled}, " +
                "disable_experimental_block_editor: ${featureState.isDisableExperimentalBlockEditorEnabled})"
    }

    private fun getFeatureFlagsString(): String {
        return getFeatureFlagsString(gutenbergKitFeatureChecker.getFeatureState())
    }

    private fun logEditorDecision(
        shouldUseGutenberg: Boolean,
        isNewPost: Boolean,
        postContent: String,
        post: PostModel?,
        site: SiteModel
    ) {
        val hasGutenbergBlocks = PostUtils.contentContainsGutenbergBlocks(postContent)
        val isBlockEditorDefaultForNewPosts = SiteUtils.isBlockEditorDefaultForNewPost(site)

        val postInfo = if (post != null) {
            "post_id: ${post.id}, remote_id: ${post.remotePostId}, is_local_draft: ${post.isLocalDraft}, " +
                    "content_length: ${postContent.length}, has_blocks: $hasGutenbergBlocks"
        } else {
            "new_post: true"
        }

        val siteInfo = "site_id: ${site.id}, site_url: ${site.url}, " +
                "block_editor_default_for_new_posts: $isBlockEditorDefaultForNewPosts"

        val reason = if (shouldUseGutenberg) {
            when {
                isNewPost -> "GutenbergKit feature is enabled and this is a new post with block editor as site default"
                hasGutenbergBlocks -> "GutenbergKit feature is enabled and the existing post contains Gutenberg blocks"
                else -> "GutenbergKit feature is enabled and PostUtils.shouldShowGutenbergEditor returned true"
            }
        } else {
            when {
                !isNewPost && !hasGutenbergBlocks -> 
                    "GutenbergKit feature is enabled but this existing post has no Gutenberg blocks"
                isNewPost && !isBlockEditorDefaultForNewPosts -> 
                    "GutenbergKit feature is enabled but site doesn't default to block editor for new posts"
                else -> "GutenbergKit feature is enabled but PostUtils.shouldShowGutenbergEditor returned false"
            }
        }

        val action = if (shouldUseGutenberg) "is being used" else "is NOT being used"
        AppLog.d(AppLog.T.EDITOR, "GutenbergKit editor $action because $reason ($postInfo, $siteInfo)")
    }

    private fun getPostFromParams(params: EditorLauncherParams): PostModel? {
        return params.postLocalId?.let { localId ->
            postStore.getPostByLocalPostId(localId)
        }
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
        when (params.siteSource) {
            is EditorLauncherSiteSource.DirectSite -> putExtra(
                WordPress.SITE, params.siteSource.siteModel
            )

            is EditorLauncherSiteSource.QuickPressSiteId -> putExtra(
                EditorConstants.EXTRA_QUICKPRESS_BLOG_ID,
                params.siteSource.siteId
            )
        }
        params.isPage?.let { putExtra(EditorConstants.EXTRA_IS_PAGE, it) }
        params.isPromo?.let { putExtra(EditorConstants.EXTRA_IS_PROMO, it) }
        putExtra(EXTRA_LAUNCHED_VIA_EDITOR_LAUNCHER, true)
    }

    private fun Intent.addPostExtras(params: EditorLauncherParams) {
        params.postLocalId?.let { putExtra(EditorConstants.EXTRA_POST_LOCAL_ID, it) }
        params.postRemoteId?.let { putExtra(EditorConstants.EXTRA_POST_REMOTE_ID, it) }
        params.loadAutoSaveRevision?.let { putExtra(EditorConstants.EXTRA_LOAD_AUTO_SAVE_REVISION, it) }
        params.isQuickPress?.let { putExtra(EditorConstants.EXTRA_IS_QUICKPRESS, it) }
        params.isLandingEditor?.let { putExtra(EditorConstants.EXTRA_IS_LANDING_EDITOR, it) }
        params.isLandingEditorOpenedForNewSite?.let {
            putExtra(EditorConstants.EXTRA_IS_LANDING_EDITOR_OPENED_FOR_NEW_SITE, it)
        }
    }

    private fun Intent.addReblogExtras(params: EditorLauncherParams) {
        params.reblogPostTitle?.let { putExtra(EditorConstants.EXTRA_REBLOG_POST_TITLE, it) }
        params.reblogPostQuote?.let { putExtra(EditorConstants.EXTRA_REBLOG_POST_QUOTE, it) }
        params.reblogPostImage?.let { putExtra(EditorConstants.EXTRA_REBLOG_POST_IMAGE, it) }
        params.reblogPostCitation?.let { putExtra(EditorConstants.EXTRA_REBLOG_POST_CITATION, it) }
        params.reblogAction?.let { action = it }
    }

    private fun Intent.addPageExtras(params: EditorLauncherParams) {
        params.pageTitle?.let { putExtra(EditorConstants.EXTRA_PAGE_TITLE, it) }
        params.pageContent?.let { putExtra(EditorConstants.EXTRA_PAGE_CONTENT, it) }
        params.pageTemplate?.let { putExtra(EditorConstants.EXTRA_PAGE_TEMPLATE, it) }
    }

    private fun Intent.addMiscExtras(params: EditorLauncherParams) {
        params.voiceContent?.let { putExtra(EditorConstants.EXTRA_VOICE_CONTENT, it) }
        params.insertMedia?.let { putExtra(EditorConstants.EXTRA_INSERT_MEDIA, it) }
        params.source?.let { putExtra(AnalyticsUtils.EXTRA_CREATION_SOURCE_DETAIL, it) }
        params.promptId?.let { putExtra(EditorConstants.EXTRA_PROMPT_ID, it) }
        params.entryPoint?.let { putExtra(EditorConstants.EXTRA_ENTRY_POINT, it) }
    }
}
