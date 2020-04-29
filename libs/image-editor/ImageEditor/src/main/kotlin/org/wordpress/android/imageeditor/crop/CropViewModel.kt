package org.wordpress.android.imageeditor.crop

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import org.wordpress.android.imageeditor.ImageEditor
import org.wordpress.android.imageeditor.ImageEditor.Action
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropDoneMenuClicked
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropOpened
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.CropSuccessful
import org.wordpress.android.imageeditor.ImageEditor.EditorAction.EditorFinishedEditing
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveFailedState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveStartState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveSuccessState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiLoadedState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiStartLoadingWithBundleState
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.OutputData
import org.wordpress.android.imageeditor.viewmodel.Event
import java.io.File
import java.io.Serializable

class CropViewModel : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _cropAndSaveImageStateEvent = MutableLiveData<Event<ImageCropAndSaveState>>()
    val cropAndSaveImageStateEvent: LiveData<Event<ImageCropAndSaveState>> = _cropAndSaveImageStateEvent

    private val _navigateBackWithCropResult = MutableLiveData<CropResult>()
    val navigateBackWithCropResult: LiveData<CropResult> = _navigateBackWithCropResult

    private lateinit var mediaEditingDirectory: File
    private lateinit var inputFilePath: String
    private lateinit var outputFileExtension: String
    private lateinit var imageEditor: ImageEditor
    private var shouldReturnToPreviewScreen: Boolean = false
    private var isStarted = false

    private val cropOptions by lazy {
        Options().apply {
            setShowCropGrid(true)
            setFreeStyleCropEnabled(true)
            setShowCropFrame(true)
            setHideBottomControls(false)
            // If not set, uCrop takes its default compress format: JPEG
            setCompressionFormat(
                when {
                    outputFileExtension.equals(PNG, ignoreCase = true) -> Bitmap.CompressFormat.PNG
                    outputFileExtension.equals(WEBP, ignoreCase = true) -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }
            )
            setCompressionQuality(COMPRESS_QUALITY_100) // If not set, uCrop takes its default compress quality: 90
        }
    }

    private val cropOptionsBundleWithFilesInfo by lazy {
        Bundle().apply {
            putParcelable(UCrop.EXTRA_INPUT_URI, Uri.fromFile(File(inputFilePath)))

            putParcelable(
                UCrop.EXTRA_OUTPUT_URI,
                Uri.fromFile(
                    File(
                        mediaEditingDirectory,
                        "$IMAGE_EDITOR_OUTPUT_IMAGE_FILE_NAME${inputFilePath.hashCode()}.$outputFileExtension"
                    )
                )
            )
            putAll(cropOptions.optionBundle)
        }
    }

    fun start(
        inputFilePath: String,
        outputFileExtension: String?,
        shouldReturnToPreviewScreen: Boolean,
        cacheDir: File,
        imageEditor: ImageEditor
    ) {
        if (isStarted) {
            return
        }
        initMediaEditingDirectory(cacheDir)
        this.imageEditor = imageEditor
        this.inputFilePath = inputFilePath
        this.outputFileExtension = outputFileExtension ?: DEFAULT_FILE_EXTENSION
        this.shouldReturnToPreviewScreen = shouldReturnToPreviewScreen

        this.imageEditor.onEditorAction(CropOpened)

        updateUiState(UiStartLoadingWithBundleState(cropOptionsBundleWithFilesInfo))
        isStarted = true
    }

    private fun initMediaEditingDirectory(cacheDir: File) {
        mediaEditingDirectory = File(cacheDir, MEDIA_EDITING)
        if (mediaEditingDirectory.mkdir()) {
            Log.d(TAG, "Cache directory created for media editing")
        }
    }

    fun onLoadingProgress(loading: Boolean) {
        if (!loading) {
            updateUiState(UiLoadedState)
        }
    }

    fun onDoneMenuClicked() {
        ImageEditor.actions.add(Action.Crop)
        imageEditor.onEditorAction(CropDoneMenuClicked(OutputData(getOutputPath())))

        updateImageCropAndSaveState(ImageCropAndSaveStartState)
    }

    fun onCropFinish(cropResultCode: Int, cropResultData: Intent) {
        val cropResult = createCropResult(cropResultCode, cropResultData)
        when (cropResult.resultCode) {
            RESULT_OK -> onCropAndSaveImageSuccess(cropResult)
            UCrop.RESULT_ERROR -> onCropAndSaveImageFailure(cropResult)
        }
    }

    private fun onCropAndSaveImageSuccess(cropResult: CropResult) {
        updateImageCropAndSaveState(ImageCropAndSaveSuccessState(cropResult))

        with(imageEditor) {
            onEditorAction(CropSuccessful(cropResult))
            if (!shouldReturnToPreviewScreen) {
                onEditorAction(EditorFinishedEditing(getOutputData(cropResult), ImageEditor.actions))
            }
        }

        _navigateBackWithCropResult.value = cropResult
    }

    private fun onCropAndSaveImageFailure(cropResult: CropResult) {
        val errorMsg = getCropError(cropResult.data)
        updateImageCropAndSaveState(ImageCropAndSaveFailedState(errorMsg, R.string.error_failed_to_crop_and_save_image))
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    private fun updateImageCropAndSaveState(state: ImageCropAndSaveState) {
        _cropAndSaveImageStateEvent.value = Event(state)
    }

    private fun createCropResult(cropResultCode: Int, cropData: Intent) = CropResult(cropResultCode, cropData)

    private fun getCropError(resultData: Intent): String? = UCrop.getError(resultData)?.message

    private fun getOutputPath(): String =
        cropOptionsBundleWithFilesInfo.getParcelable<Uri?>(UCrop.EXTRA_OUTPUT_URI)?.path ?: ""

    fun getOutputData(cropResult: CropResult): ArrayList<OutputData> {
        val imageUri: Uri? = cropResult.data.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI)

        return if (imageUri != null) {
            arrayListOf(OutputData(imageUri.toString()))
        } else {
            arrayListOf()
        }
    }

    data class CropResult(val resultCode: Int, val data: Intent) : Serializable

    sealed class UiState(
        val doneMenuVisible: Boolean = false
    ) {
        data class UiStartLoadingWithBundleState(val bundle: Bundle) : UiState(doneMenuVisible = false)
        object UiLoadedState : UiState(doneMenuVisible = true)
    }

    sealed class ImageCropAndSaveState {
        object ImageCropAndSaveStartState : ImageCropAndSaveState()
        data class ImageCropAndSaveSuccessState(val cropResult: CropResult) : ImageCropAndSaveState()
        data class ImageCropAndSaveFailedState(val errorMsg: String?, val errorResId: Int) : ImageCropAndSaveState()
    }

    companion object {
        private val TAG = CropViewModel::class.java.simpleName
        private const val IMAGE_EDITOR_OUTPUT_IMAGE_FILE_NAME = "image_editor_output_image"
        private const val DEFAULT_FILE_EXTENSION = "jpg"
        private const val COMPRESS_QUALITY_100 = 100
        private const val PNG = "png"
        private const val WEBP = "webp"
        private const val MEDIA_EDITING = "media_editing"
    }
}
