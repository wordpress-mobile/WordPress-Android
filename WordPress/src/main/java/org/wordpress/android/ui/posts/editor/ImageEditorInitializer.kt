package org.wordpress.android.ui.posts.editor

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.EditorAction
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropDoneMenuClicked
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropOpened
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropSuccessful
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorCancelled
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorShown
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorFinishedEditing
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.PreviewCropMenuClicked
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.PreviewImageSelected
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.PreviewInsertImagesClicked
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageManager.RequestListener
import org.wordpress.android.util.image.ImageType.IMAGE
import java.io.File

class ImageEditorInitializer {
    companion object {
        private const val IMAGE_STRING_URL_MSG = "ImageEditor requires a not-null string image url."
        private const val ACTIONS = "actions"
        private const val NUMBER_OF_IMAGES = "number_of_images"

        fun init(imageManager: ImageManager) {
            ImageEditor.init(
                loadIntoImageViewWithResultListener(imageManager),
                loadIntoFileWithResultListener(imageManager),
                loadIntoImageView(imageManager),
                onEditorAction()
            )
        }

        private fun loadIntoImageViewWithResultListener(
            imageManager: ImageManager
        ): (String, ImageView, ScaleType, String?, ImageEditor.RequestListener<Drawable>) -> Unit =
                { imageUrl, imageView, scaleType, thumbUrl, listener ->
                    imageManager.loadWithResultListener(
                        imageView,
                        IMAGE,
                        imageUrl,
                        scaleType,
                        thumbUrl,
                        object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: Exception?, model: Any?) = onLoadFailed(model, listener, e)
                            override fun onResourceReady(resource: Drawable, model: Any?) =
                                onResourceReady(model, listener, resource)
                        }
                    )
                }

        private fun loadIntoFileWithResultListener(imageManager: ImageManager):
                (Uri, ImageEditor.RequestListener<File>) -> Unit = { imageUri, listener ->
            imageManager.loadIntoFileWithResultListener(
                imageUri,
                object : RequestListener<File> {
                    override fun onLoadFailed(e: Exception?, model: Any?) = onLoadFailed(model, listener, e)
                    override fun onResourceReady(resource: File, model: Any?) =
                        onResourceReady(model, listener, resource)
                }
            )
        }

        private fun loadIntoImageView(imageManager: ImageManager):
            (String, ImageView, ScaleType) -> Unit = { imageUrl, imageView, scaleType ->
            imageManager.load(imageView, IMAGE, imageUrl, scaleType)
        }

        private fun <T : Any> onResourceReady(model: Any?, listener: ImageEditor.RequestListener<T>, resource: T) =
            if (model != null && (model is String || model is Uri)) {
                listener.onResourceReady(resource, model.toString())
            } else {
                throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
            }

        private fun <T : Any> onLoadFailed(model: Any?, listener: ImageEditor.RequestListener<T>, e: Exception?) =
            if (model != null && (model is String || model is Uri)) {
                listener.onLoadFailed(e, model.toString())
            } else {
                throw(IllegalArgumentException(IMAGE_STRING_URL_MSG))
            }

        private fun onEditorAction(): (EditorAction) -> Unit = { action ->
            trackEditorAction(action)
        }

        private fun trackEditorAction(action: EditorAction) {
            val stat = when (action) {
                is EditorShown -> Stat.MEDIA_EDITOR_SHOWN
                is EditorCancelled -> Stat.MEDIA_EDITOR_CANCELLED
                is EditorFinishedEditing -> Stat.MEDIA_EDITOR_USED
                is PreviewImageSelected -> Stat.MEDIA_EDITOR_PREVIEW_IMAGE_SELECTED
                is PreviewInsertImagesClicked -> Stat.MEDIA_EDITOR_PREVIEW_INSERT_IMAGES_CLICKED
                is PreviewCropMenuClicked -> Stat.MEDIA_EDITOR_PREVIEW_CROP_MENU_CLICKED
                is CropOpened -> Stat.MEDIA_EDITOR_CROP_OPENED
                is CropDoneMenuClicked -> Stat.MEDIA_EDITOR_CROP_DONE_MENU_CLICKED
                is CropSuccessful -> Stat.MEDIA_EDITOR_CROP_SUCCESSFUL
            }

            val properties = when (action) {
                is EditorShown -> mapOf(NUMBER_OF_IMAGES to action.numOfImages)
                is EditorFinishedEditing -> if (action.actions.isNotEmpty()) {
                    mapOf(ACTIONS to action.actions.map { it.label })
                } else {
                    null
                }
                is EditorCancelled,
                is PreviewImageSelected,
                is PreviewInsertImagesClicked,
                is PreviewCropMenuClicked,
                is CropOpened,
                is CropDoneMenuClicked,
                is CropSuccessful -> null
            }

            val noEditActionPerformed = stat == Stat.MEDIA_EDITOR_USED && properties == null
            if (noEditActionPerformed) {
                return
            }

            if (properties == null) {
                AnalyticsTracker.track(stat)
            } else {
                AnalyticsTracker.track(stat, properties)
            }
        }
    }
}
