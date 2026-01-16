@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import dagger.Reusable
import org.wordpress.android.editor.AztecEditorFragment
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.ui.suggestion.SuggestionActivity
import org.wordpress.android.util.WPMediaUtils
import java.util.function.Consumer
import javax.inject.Inject

/**
 * Handles activity results for the post editor.
 *
 * This class extracts activity result handling logic from EditPostActivity to improve
 * testability and separation of concerns.
 */
@Reusable
class EditorActivityResultHandler @Inject constructor(
    private val editorMedia: EditorMedia,
    private val imageEditorTracker: ImageEditorTracker
) {
    /**
     * Listener for activity result events that require Activity-level handling.
     */
    interface ActivityResultListener {
        fun getEditorFragment(): EditorFragmentAbstract?
        fun navigateToEditor()
        fun triggerLoadRevision()
        fun getRevisionFromBundle(): Any?
        fun clearSuggestionCallback()
        fun getSuggestionCallback(): Consumer<String?>?
        fun handleLastTakenPicture()
    }

    private var listener: ActivityResultListener? = null

    /**
     * Initializes the handler with the required listener.
     */
    fun start(listener: ActivityResultListener) {
        this.listener = listener
    }

    /**
     * Handles a non-OK result from an activity.
     *
     * @param requestCode The request code of the activity.
     * @return true if the result was handled.
     */
    fun handleNotOKResult(requestCode: Int): Boolean {
        // for all media related intents, let editor fragment know about cancellation
        return when (requestCode) {
            RequestCodes.MULTI_SELECT_MEDIA_PICKER,
            RequestCodes.SINGLE_SELECT_MEDIA_PICKER,
            RequestCodes.PHOTO_PICKER,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT,
            RequestCodes.MEDIA_LIBRARY,
            RequestCodes.PICTURE_LIBRARY,
            RequestCodes.TAKE_PHOTO,
            RequestCodes.VIDEO_LIBRARY,
            RequestCodes.TAKE_VIDEO,
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK -> {
                listener?.getEditorFragment()?.mediaSelectionCancelled()
                true
            }
            else -> false
        }
    }

    /**
     * Handles an OK result from an activity.
     *
     * @param requestCode The request code of the activity.
     * @param data The intent data returned from the activity.
     * @return true if the result was handled.
     */
    @Suppress("ReturnCount", "LongMethod")
    fun handleOKResult(requestCode: Int, data: Intent?): Boolean {
        return when (requestCode) {
            RequestCodes.MULTI_SELECT_MEDIA_PICKER,
            RequestCodes.SINGLE_SELECT_MEDIA_PICKER -> {
                handleMediaPickerResult(data)
                true
            }
            RequestCodes.PHOTO_PICKER,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT -> {
                handlePhotoPickerResult(data)
                true
            }
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK -> {
                handleStockMediaPickerSingleSelect(data)
                true
            }
            RequestCodes.MEDIA_LIBRARY,
            RequestCodes.PICTURE_LIBRARY,
            RequestCodes.VIDEO_LIBRARY -> {
                handleLibraries(data)
                true
            }
            RequestCodes.TAKE_PHOTO -> {
                addLastTakenPicture()
                true
            }
            RequestCodes.TAKE_VIDEO -> {
                handleTakeVideo(data)
                true
            }
            RequestCodes.MEDIA_SETTINGS -> {
                handleMediaSettings(data)
                true
            }
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT -> {
                handleStockMediaPickerMultiSelect(data)
                true
            }
            RequestCodes.GIF_PICKER_SINGLE_SELECT,
            RequestCodes.GIF_PICKER_MULTI_SELECT -> {
                handleGifPicker(data)
                true
            }
            RequestCodes.HISTORY_DETAIL -> {
                handleHistoryDetail()
                true
            }
            RequestCodes.IMAGE_EDITOR_EDIT_IMAGE -> {
                handleImageEditor(data)
                true
            }
            RequestCodes.SELECTED_USER_MENTION -> {
                handleUserMention(data)
                true
            }
            RequestCodes.FILE_LIBRARY,
            RequestCodes.AUDIO_LIBRARY -> {
                handleFileOrAudioLibrary(data)
                true
            }
            else -> false
        }
    }

    private fun handleStockMediaPickerSingleSelect(data: Intent?) {
        if (data?.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID) == true) {
            // pass array with single item
            val mediaIds = longArrayOf(data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0))
            editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.STOCK_PHOTO_LIBRARY, mediaIds)
        }
    }

    private fun handleLibraries(data: Intent?) {
        editorMedia.addNewMediaItemsToEditorAsync(
            WPMediaUtils.retrieveMediaUris(data),
            false
        )
    }

    private fun handleTakeVideo(data: Intent?) {
        data?.data?.let {
            editorMedia.addNewMediaToEditorAsync(it, true)
        }
    }

    private fun handleMediaSettings(data: Intent?) {
        val editorFragment = listener?.getEditorFragment()
        if (editorFragment is AztecEditorFragment) {
            editorFragment.onActivityResult(
                AztecEditorFragment.EDITOR_MEDIA_SETTINGS,
                Activity.RESULT_OK,
                data
            )
        }
    }

    private fun handleStockMediaPickerMultiSelect(data: Intent?) {
        val key = MediaBrowserActivity.RESULT_IDS
        if (data?.hasExtra(key) == true) {
            val mediaIds: LongArray? = data.getLongArrayExtra(key)
            mediaIds?.let {
                editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.STOCK_PHOTO_LIBRARY, it)
            }
        }
    }

    private fun handleGifPicker(data: Intent?) {
        val localIds = data?.getIntArrayExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS)
        if (localIds != null && localIds.isNotEmpty()) {
            editorMedia.addGifMediaToPostAsync(localIds)
        }
    }

    private fun handleHistoryDetail() {
        val revision = listener?.getRevisionFromBundle()

        if (revision != null) {
            listener?.navigateToEditor()
            Handler(Looper.getMainLooper()).postDelayed(
                { listener?.triggerLoadRevision() },
                HISTORY_DETAIL_DELAY_MS
            )
        }
    }

    companion object {
        private const val HISTORY_DETAIL_DELAY_MS = 300L
    }

    private fun handleImageEditor(data: Intent?) {
        val uris: List<Uri> = WPMediaUtils.retrieveImageEditorResult(data)
        imageEditorTracker.trackAddPhoto(uris)
        uris.forEach { item ->
            editorMedia.addNewMediaToEditorAsync(item, false)
        }
    }

    private fun handleUserMention(data: Intent?) {
        val callback = listener?.getSuggestionCallback()
        if (callback != null) {
            val selectedMention: String? = data?.getStringExtra(SuggestionActivity.SELECTED_VALUE)
            callback.accept(selectedMention)
            listener?.clearSuggestionCallback()
        }
    }

    private fun handleFileOrAudioLibrary(data: Intent?) {
        val uriList = WPMediaUtils.retrieveMediaUris(data)
        for (uri in uriList) {
            editorMedia.addNewMediaToEditorAsync(uri, false)
        }
    }

    private fun addLastTakenPicture() {
        listener?.handleLastTakenPicture()
    }

    private fun handlePhotoPickerResult(data: Intent?) {
        data ?: return

        if (data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)) {
            val uriList: List<Uri> = convertStringArrayIntoUrisList(
                data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
            )
            editorMedia.addNewMediaItemsToEditorAsync(uriList, false)
        } else if (data.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID)) {
            val mediaId = data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0)
            editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, longArrayOf(mediaId))
        }
    }

    @Suppress("ReturnCount")
    private fun handleMediaPickerResult(data: Intent?) {
        data ?: return

        if (data.hasExtra(MediaBrowserActivity.RESULT_IDS)) {
            val ids = data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS) ?: return
            if (ids.isEmpty()) return

            editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, ids)
        }
    }

    private fun convertStringArrayIntoUrisList(stringArray: Array<String>?): List<Uri> {
        return stringArray?.mapNotNull { Uri.parse(it) } ?: emptyList()
    }
}
