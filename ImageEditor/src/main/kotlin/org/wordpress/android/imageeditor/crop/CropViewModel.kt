package org.wordpress.android.imageeditor.crop

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import java.io.File
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveFailedState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveStartState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveSuccessState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiStartLoadingWithBundleState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiLoadedState

class CropViewModel : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _cropAndSaveImageState = MutableLiveData<ImageCropAndSaveState>()
    val cropAndSaveImageState: LiveData<ImageCropAndSaveState> = _cropAndSaveImageState

    private val _navigateBackWithCropResult = MutableLiveData<CropResult>()
    val navigateBackWithCropResult: LiveData<CropResult> = _navigateBackWithCropResult

    private lateinit var cacheDir: File
    private lateinit var inputFilePath: String
    private lateinit var outputFileExtension: String
    private var isStarted = false

    private val cropOptions by lazy {
        Options().also {
            with(it) {
                setShowCropGrid(true)
                setFreeStyleCropEnabled(true)
                setShowCropFrame(true)
                setHideBottomControls(false)
            }
        }
    }

    private val cropOptionsBundleWithFilesInfo by lazy {
        Bundle().also {
            with(it) {
                putParcelable(UCrop.EXTRA_INPUT_URI, Uri.fromFile(File(inputFilePath)))
                putParcelable(UCrop.EXTRA_OUTPUT_URI, Uri.fromFile(
                            File(cacheDir,
                            "$IMAGE_EDITOR_OUTPUT_IMAGE_FILE_NAME.$outputFileExtension"
                        )))
                putAll(cropOptions.optionBundle)
            }
        }
    }

    fun start(inputFilePath: String, outputFileExtension: String?, cacheDir: File) {
        if (isStarted) {
            return
        }
        this.cacheDir = cacheDir
        this.inputFilePath = inputFilePath
        this.outputFileExtension = outputFileExtension ?: DEFAULT_FILE_EXTENSION

        updateUiState(UiStartLoadingWithBundleState(cropOptionsBundleWithFilesInfo))
        isStarted = true
    }

    fun onLoadingProgress(loading: Boolean) {
        if (!loading) {
            updateUiState(UiLoadedState)
        }
    }

    fun onDoneMenuClicked() {
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
        _cropAndSaveImageState.value = state
    }

    private fun createCropResult(cropResultCode: Int, cropData: Intent) = CropResult(cropResultCode, cropData)

    private fun getCropError(resultData: Intent): String? = UCrop.getError(resultData)?.message

    data class CropResult(val resultCode: Int, val data: Intent)

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
        const val IMAGE_EDITOR_OUTPUT_IMAGE_FILE_NAME = "image_editor_output_image"
        const val DEFAULT_FILE_EXTENSION = "jpg"
    }
}
