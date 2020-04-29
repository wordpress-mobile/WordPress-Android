package org.wordpress.android.imageeditor

import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import org.wordpress.android.imageeditor.crop.CropViewModel.CropResult
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.OutputData
import java.io.File

class ImageEditor private constructor(
    private val loadIntoImageViewWithResultListener: (
        (String, ImageView, ScaleType, String?, RequestListener<Drawable>) -> Unit
    ),
    private val loadIntoFileWithResultListener: ((Uri, RequestListener<File>) -> Unit),
    private val loadIntoImageView: ((String, ImageView, ScaleType) -> Unit),
    private val onEditorAction: ((EditorAction) -> Unit)
) {
    interface RequestListener<T> {
        /**
         * Called when an exception occurs during an image load
         *
         * @param e The maybe {@code null} exception containing information about why the request failed.
         * @param url The url of the image we were trying to load when the exception occurred.
         */
        fun onLoadFailed(e: Exception?, url: String)

        /**
         * Called when a load completes successfully
         *
         * @param resource The resource that was loaded for the target.
         * @param url The specific url that was used to load the image.
         */
        fun onResourceReady(resource: T, url: String)
    }

    fun loadIntoImageViewWithResultListener(
        imageUrl: String,
        imageView: ImageView,
        scaleType: ScaleType,
        thumbUrl: String?,
        listener: RequestListener<Drawable>
    ) {
        loadIntoImageViewWithResultListener.invoke(imageUrl, imageView, scaleType, thumbUrl, listener)
    }

    fun loadIntoFileWithResultListener(
        imageUri: Uri,
        listener: RequestListener<File>
    ) {
        loadIntoFileWithResultListener.invoke(imageUri, listener)
    }

    fun loadIntoImageView(imageUrl: String, imageView: ImageView, scaleType: ScaleType) {
        loadIntoImageView.invoke(imageUrl, imageView, scaleType)
    }

    fun onEditorAction(action: EditorAction) {
        onEditorAction.invoke(action)
    }

    sealed class EditorAction {
        // General actions
        data class EditorShown(val numOfImages: Int) : EditorAction()
        object EditorCancelled : EditorAction()
        data class EditorFinishedEditing(val outputDataList: List<OutputData>) :
                EditorAction()

        // Preview screen actions
        data class PreviewImageSelected(val highResImageUrl: String, val selectedPosition: Int) : EditorAction()
        data class PreviewInsertImagesClicked(val outputDataList: List<OutputData>) : EditorAction()
        object PreviewCropMenuClicked : EditorAction()

        // Crop screen actions
        object CropOpened : EditorAction()
        data class CropDoneMenuClicked(val outputData: OutputData) : EditorAction()
        data class CropSuccessful(val cropResult: CropResult) : EditorAction()
    }

    companion object {
        private lateinit var INSTANCE: ImageEditor

        val instance: ImageEditor get() = INSTANCE

        fun init(
            loadIntoImageViewWithResultListener: (
                (String, ImageView, ScaleType, String?, RequestListener<Drawable>) -> Unit
            ),
            loadIntoFileWithResultListener: ((Uri, RequestListener<File>) -> Unit),
            loadIntoImageView: ((String, ImageView, ScaleType) -> Unit),
            onEditorAction: ((EditorAction) -> Unit)
        ) {
            INSTANCE = ImageEditor(
                loadIntoImageViewWithResultListener,
                loadIntoFileWithResultListener,
                loadIntoImageView,
                onEditorAction
            )
        }
    }
}
