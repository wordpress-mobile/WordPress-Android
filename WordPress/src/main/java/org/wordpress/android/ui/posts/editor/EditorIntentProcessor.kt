@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.posts.EditorConstants
import org.wordpress.android.ui.posts.EditorUnitFunctions
import org.wordpress.android.util.AutolinkUtils
import org.wordpress.android.util.ReblogUtils
import org.wordpress.android.util.ShortcutUtils
import javax.inject.Inject

/**
 * Processes intents for the post editor, determining what action should be taken
 * based on the intent's action, extras, and data.
 *
 * This class extracts intent processing logic from EditPostActivity to improve
 * testability and separation of concerns.
 */
@Reusable
class EditorIntentProcessor @Inject constructor(
    private val siteStore: SiteStore,
    private val reblogUtils: ReblogUtils,
    private val shortcutUtils: ShortcutUtils
) {
    /**
     * Represents the result of processing an editor intent.
     */
    sealed class IntentProcessResult {
        /**
         * Create a new post with optional title and content.
         */
        data class NewPost(
            val title: String? = null,
            val content: String? = null,
            val isPage: Boolean = false
        ) : IntentProcessResult()

        /**
         * Load an existing post by its local ID.
         */
        data class ExistingPost(
            val localPostId: Int,
            val loadAutoSaveRevision: Boolean = false
        ) : IntentProcessResult()

        /**
         * Create a new post from a share action with text content.
         */
        data class ShareActionText(
            val title: String?,
            val content: String
        ) : IntentProcessResult()

        /**
         * Create a new post from a share action with media.
         */
        data class ShareActionMedia(
            val mediaUris: List<Uri>
        ) : IntentProcessResult()

        /**
         * Create a new post from a reblog action.
         */
        data class Reblog(
            val title: String?,
            val content: String
        ) : IntentProcessResult()

        /**
         * Create a new page from a layout picker selection.
         */
        data class PageFromLayout(
            val title: String?,
            val content: String?
        ) : IntentProcessResult()

        /**
         * The intent was invalid or missing required data.
         */
        object InvalidIntent : IntentProcessResult()
    }

    /**
     * Processes an intent and returns the appropriate action to take.
     *
     * @param intent The intent to process.
     * @param extras The intent's extras bundle (can be null).
     * @param siteModel The current site model.
     * @param isRestarting Whether the editor is restarting (e.g., after editor switch).
     * @return An [IntentProcessResult] indicating what action should be taken.
     */
    @Suppress("ReturnCount")
    fun processIntent(
        intent: Intent,
        extras: Bundle?,
        siteModel: SiteModel,
        isRestarting: Boolean
    ): IntentProcessResult {
        if (extras == null) {
            return IntentProcessResult.InvalidIntent
        }

        val action = intent.action
        val hasLocalPostId = extras.containsKey(EditorConstants.EXTRA_POST_LOCAL_ID)
        val isActionSendOrNewMedia = EditorUnitFunctions.isActionSendOrNewMedia(action)
        val isQuickPress = extras.containsKey(EditorConstants.EXTRA_IS_QUICKPRESS)
        val isPage = extras.getBoolean(EditorConstants.EXTRA_IS_PAGE)

        // Check if we should create a new post vs load an existing one
        if (!hasLocalPostId || isActionSendOrNewMedia || isQuickPress) {
            // Creating a new post
            return processNewPostIntent(intent, extras, siteModel, isPage)
        } else {
            // Loading an existing post
            return processExistingPostIntent(extras, isRestarting)
        }
    }

    /**
     * Gets a site model for QuickPress if one was specified in the intent.
     *
     * @param intent The editor intent.
     * @param extras The intent's extras bundle.
     * @return The site model for QuickPress, or null if not applicable.
     */
    fun getSiteForQuickPress(intent: Intent, extras: Bundle?): SiteModel? {
        if (extras == null || extras.containsKey(EditorConstants.EXTRA_POST_LOCAL_ID)) {
            return null
        }

        val isActionSendOrNewMedia = EditorUnitFunctions.isActionSendOrNewMedia(intent.action)
        val hasQuickPressFlag = extras.containsKey(EditorConstants.EXTRA_IS_QUICKPRESS)
        val hasQuickPressBlogId = extras.containsKey(EditorConstants.EXTRA_QUICKPRESS_BLOG_ID)

        return if ((isActionSendOrNewMedia || hasQuickPressFlag) && hasQuickPressBlogId) {
            val localSiteId = intent.getIntExtra(EditorConstants.EXTRA_QUICKPRESS_BLOG_ID, -1)
            siteStore.getSiteByLocalId(localSiteId)
        } else {
            null
        }
    }

    /**
     * Extracts media URIs from a share intent.
     *
     * @param intent The share intent.
     * @return A list of media URIs, or empty if none found.
     */
    fun extractMediaUrisFromShareIntent(intent: Intent): List<Uri> {
        if (!intent.hasExtra(Intent.EXTRA_STREAM)) {
            return emptyList()
        }

        val action = intent.action
        val sharedUris = mutableListOf<Uri>()

        if (Intent.ACTION_SEND_MULTIPLE == action) {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { uri ->
                if (EditorUnitFunctions.isMediaTypeIntent(intent, uri)) {
                    sharedUris.add(uri)
                }
            }
        } else {
            // Single media share - only allow images and video types
            if (EditorUnitFunctions.isMediaTypeIntent(intent, null)) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    sharedUris.add(it)
                }
            }
        }

        return sharedUris
    }

    /**
     * Reports that a shortcut was used to create a new post.
     */
    fun reportNewPostShortcutUsed() {
        shortcutUtils.reportShortcutUsed(org.wordpress.android.ui.Shortcut.CREATE_NEW_POST)
    }

    @Suppress("ReturnCount")
    private fun processNewPostIntent(
        intent: Intent,
        extras: Bundle,
        siteModel: SiteModel,
        isPage: Boolean
    ): IntentProcessResult {
        val action = intent.action

        // Check for page from layout picker
        if (isPage && !TextUtils.isEmpty(extras.getString(EditorConstants.EXTRA_PAGE_TITLE))) {
            val title = extras.getString(EditorConstants.EXTRA_PAGE_TITLE)
            val layoutSlug = extras.getString(EditorConstants.EXTRA_PAGE_TEMPLATE)
            val content = siteStore.getBlockLayoutContent(siteModel, layoutSlug ?: "")
            return IntentProcessResult.PageFromLayout(title, content)
        }

        // Check for share action
        if (Intent.ACTION_SEND == action) {
            return processShareAction(intent)
        }

        // Check for reblog action
        if (EditorConstants.ACTION_REBLOG == action) {
            return processReblogAction(intent)
        }

        // Default: create a new empty post
        return IntentProcessResult.NewPost(isPage = isPage)
    }

    private fun processShareAction(intent: Intent): IntentProcessResult {
        // Check if this is a media share
        if (EditorUnitFunctions.isMediaTypeIntent(intent, null)) {
            val mediaUris = extractMediaUrisFromShareIntent(intent)
            return if (mediaUris.isNotEmpty()) {
                IntentProcessResult.ShareActionMedia(mediaUris)
            } else {
                IntentProcessResult.NewPost()
            }
        }

        // Text share
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val content = EditorUnitFunctions.migrateToGutenbergEditor(AutolinkUtils.autoCreateLinks(text))
        return IntentProcessResult.ShareActionText(title, content)
    }

    private fun processReblogAction(intent: Intent): IntentProcessResult {
        val title = intent.getStringExtra(EditorConstants.EXTRA_REBLOG_POST_TITLE)
        val quote = intent.getStringExtra(EditorConstants.EXTRA_REBLOG_POST_QUOTE)
        val citation = intent.getStringExtra(EditorConstants.EXTRA_REBLOG_POST_CITATION)
        val image = intent.getStringExtra(EditorConstants.EXTRA_REBLOG_POST_IMAGE)
        val content = reblogUtils.reblogContent(image, quote ?: "", title, citation)
        return IntentProcessResult.Reblog(title, content)
    }

    private fun processExistingPostIntent(extras: Bundle, isRestarting: Boolean): IntentProcessResult {
        val localPostId = extras.getInt(EditorConstants.EXTRA_POST_LOCAL_ID)

        // If restarting and no post found, we'll need to create a new one
        // This is handled by the Activity based on whether the post is found
        if (localPostId == 0 && isRestarting) {
            return IntentProcessResult.NewPost()
        }

        val loadAutoSaveRevision = extras.getBoolean(EditorConstants.EXTRA_LOAD_AUTO_SAVE_REVISION)
        return IntentProcessResult.ExistingPost(localPostId, loadAutoSaveRevision)
    }
}
