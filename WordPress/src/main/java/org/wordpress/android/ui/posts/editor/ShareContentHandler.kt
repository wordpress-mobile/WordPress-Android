@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.content.Intent
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult.Updated
import org.wordpress.android.ui.posts.EditorUnitFunctions
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.util.AutolinkUtils
import javax.inject.Inject

/**
 * Handles setting post content from share actions.
 *
 * This class extracts share content handling logic from EditPostActivity to improve
 * testability and separation of concerns.
 */
@Reusable
class ShareContentHandler @Inject constructor(
    private val editorMedia: EditorMedia
) {
    /**
     * Listener for share content events that require Activity-level handling.
     */
    interface ShareContentListener {
        fun getIntent(): Intent
        fun getEditPostRepository(): EditPostRepository
        fun getEditorFragment(): EditorFragmentAbstract?
        fun isShowGutenbergEditor(): Boolean
        fun setHasSetPostContent(value: Boolean)
    }

    private var listener: ShareContentListener? = null

    /**
     * Initializes the handler with the required listener.
     */
    fun start(listener: ShareContentListener) {
        this.listener = listener
    }

    /**
     * Sets the post content from a share action intent.
     * This handles both text and media sharing.
     */
    fun setPostContentFromShareAction() {
        val intent = listener?.getIntent() ?: return
        val editPostRepository = listener?.getEditPostRepository() ?: return
        val editorFragment = listener?.getEditorFragment()
        val showGutenbergEditor = listener?.isShowGutenbergEditor() ?: false

        // Check for shared text
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        if (text != null) {
            listener?.setHasSetPostContent(true)
            editPostRepository.updateAsync({ postModel: PostModel ->
                if (title != null) {
                    postModel.setTitle(title)
                }
                // Create an <a href> element around links
                var updatedContent: String = AutolinkUtils.autoCreateLinks(text)

                // If editor is Gutenberg, add Gutenberg block around content
                if (showGutenbergEditor) {
                    updatedContent = EditorUnitFunctions.migrateToGutenbergEditor(updatedContent)
                }

                // update PostModel
                postModel.setContent(updatedContent)
                editPostRepository.updatePublishDateIfShouldBePublishedImmediately(postModel)
                true
            }) { postModel, result ->
                if (result === Updated) {
                    editorFragment?.setTitle(postModel.title)
                    editorFragment?.setContent(postModel.content)
                }
            }
        }
        setPostMediaFromShareAction()
    }

    /**
     * Adds media from a share action intent to the editor.
     */
    fun setPostMediaFromShareAction() {
        val intent = listener?.getIntent() ?: return

        // Short-circuit the method if Intent.EXTRA_STREAM is not found
        if (!intent.hasExtra(Intent.EXTRA_STREAM)) {
            return
        }

        // Check for shared media
        val action = intent.action
        val sharedUris: ArrayList<Uri> = ArrayList()

        if (Intent.ACTION_SEND_MULTIPLE == action) {
            val potentialUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            potentialUris?.forEach { uri ->
                if (EditorUnitFunctions.isMediaTypeIntent(intent, uri)) {
                    sharedUris.add(uri)
                }
            }
        } else {
            // For a single media share, we only allow images and video types
            if (EditorUnitFunctions.isMediaTypeIntent(intent, null)) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    sharedUris.add(it)
                }
            }
        }

        if (sharedUris.isNotEmpty()) {
            // removing this from the intent so it doesn't insert the media items again on each Activity re-creation
            intent.removeExtra(Intent.EXTRA_STREAM)
            editorMedia.addNewMediaItemsToEditorAsync(sharedUris, false)
        }
    }
}
