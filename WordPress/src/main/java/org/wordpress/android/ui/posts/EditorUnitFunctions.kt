package org.wordpress.android.ui.posts

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.core.util.Consumer
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.gutenberg.MediaType
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.util.AppLog

/**
 * Pure utility functions extracted from EditPostActivity.
 * These functions take parameters, return values, and don't access class state.
 * 
 * Implementation note: This is an `object` (singleton) rather than a class to prevent
 * accidental introduction of mutable state. Since these are pure functions with no 
 * side effects, they don't typically need mocking at call sites - tests can use real
 * data and verify actual behavior.
 * 
 * If mockability becomes necessary for complex test scenarios, consider refactoring to:
 * ```
 * interface EditorUnitFunctions {
 *     fun isActionSendOrNewMedia(action: String?): Boolean = ...
 *     // ... other functions with default implementations
 *     companion object : EditorUnitFunctions
 * }
 * ```
 * This would provide mockability while maintaining default implementations and 
 * discouraging stateful modifications.
 */
object EditorUnitFunctions {
    /**
     * Checks if the given action is a send or new media action.
     */
    fun isActionSendOrNewMedia(action: String?): Boolean {
        return action == Intent.ACTION_SEND
                || action == Intent.ACTION_SEND_MULTIPLE
                || action == EditorConstants.NEW_MEDIA_POST
    }

    /**
     * Converts a list of revisions to an array of revision IDs.
     */
    fun getRevisionsIds(revisions: List<Revision>): LongArray {
        val idsArray = LongArray(revisions.size)
        for (i in revisions.indices) {
            val current: Revision = revisions[i]
            idsArray[i] = current.revisionId
        }
        return idsArray
    }

    /**
     * Migrates content to Gutenberg editor format by wrapping it in a paragraph block.
     */
    fun migrateToGutenbergEditor(content: String): String {
        return "<!-- wp:paragraph --><p>$content</p><!-- /wp:paragraph -->"
    }

    /**
     * Checks if an intent contains media (image or video) content.
     */
    fun isMediaTypeIntent(intent: Intent, uri: Uri?): Boolean {
        var type: String? = null
        if (uri != null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        } else {
            type = intent.type
        }
        return type != null && (type.startsWith("image") || type.startsWith("video"))
    }

    /**
     * Converts an array of string URIs to a list of Uri objects.
     */
    fun convertStringArrayIntoUrisList(stringArray: Array<String>?): List<Uri> {
        val uris: MutableList<Uri> = ArrayList(stringArray?.size ?: 0)
        stringArray?.forEach { stringUri ->
            uris.add(stringUri.toUri())
        }
        return uris
    }

    /**
     * Creates an exception logger consumer that logs exceptions to AppLog.
     */
    fun getExceptionLogger(): Consumer<Exception> {
        return Consumer { e: Exception? ->
            AppLog.e(
                AppLog.T.EDITOR,
                e
            )
        }
    }

    /**
     * Creates a breadcrumb logger consumer that logs strings to AppLog.
     */
    fun getBreadcrumbLogger(): Consumer<String> {
        return Consumer { s: String? ->
            AppLog.e(
                AppLog.T.EDITOR,
                s
            )
        }
    }

    /**
     * Maps allowed media types to the appropriate MediaBrowserType.
     */
    fun mapAllowedTypesToMediaBrowserType(allowedTypes: Array<MediaType>, multiple: Boolean): MediaBrowserType {
        return when {
            allowedTypes.contains(MediaType.IMAGE) && allowedTypes.contains(MediaType.VIDEO) -> {
                if (multiple) MediaBrowserType.GUTENBERG_MEDIA_PICKER
                else MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER
            }

            allowedTypes.contains(MediaType.IMAGE) -> {
                if (multiple) MediaBrowserType.GUTENBERG_IMAGE_PICKER
                else MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER
            }

            allowedTypes.contains(MediaType.VIDEO) -> {
                if (multiple) MediaBrowserType.GUTENBERG_VIDEO_PICKER
                else MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER
            }

            allowedTypes.contains(MediaType.AUDIO) -> MediaBrowserType.GUTENBERG_SINGLE_AUDIO_FILE_PICKER
            else -> {
                if (multiple) MediaBrowserType.GUTENBERG_MEDIA_PICKER
                else MediaBrowserType.GUTENBERG_SINGLE_FILE_PICKER
            }
        }
    }

    /**
     * Generates HTML for displaying a failed media upload with retry functionality.
     */
    fun getUploadErrorHtml(mediaId: String, path: String, tapToTryAgainString: String): String {
        return String.format(
            java.util.Locale.US,
            ("<span id=\"img_container_%s\" class=\"img_container failed\" data-failed=\"%s\">"
                    + "<progress id=\"progress_%s\" value=\"0\" class=\"wp_media_indicator failed\" "
                    + "contenteditable=\"false\"></progress>"
                    + "<img data-wpid=\"%s\" src=\"%s\" alt=\"\" class=\"failed\"></span>"),
            mediaId, tapToTryAgainString, mediaId, mediaId, path
        )
    }
}
