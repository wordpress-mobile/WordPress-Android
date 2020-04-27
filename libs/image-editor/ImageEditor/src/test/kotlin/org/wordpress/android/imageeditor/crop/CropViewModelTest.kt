package org.wordpress.android.imageeditor.crop

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.yalantis.ucrop.UCrop
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNull
import org.junit.Rule
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveFailedState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveStartState
import org.wordpress.android.imageeditor.crop.CropViewModel.ImageCropAndSaveState.ImageCropAndSaveSuccessState
import org.wordpress.android.imageeditor.crop.CropViewModel.UiState.UiStartLoadingWithBundleState
import java.io.File

private const val TEST_INPUT_IMAGE_PATH = "/input/file/path"
private const val TEST_OUTPUT_FILE_EXTENSION = "jpg"

class CropViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()
    private val cacheDir = File("/cache/dir")

    // Class under test
    private lateinit var viewModel: CropViewModel
    private val cropSuccessResultCode = Activity.RESULT_OK
    private val cropFailedResultCode = UCrop.RESULT_ERROR
    private val cropResultData = Intent()

    @Before
    fun setUp() {
        viewModel = CropViewModel()
    }

    @Test
    fun `ui starts loading on start`() {
        initViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(UiStartLoadingWithBundleState::class.java)
    }

    @Test
    fun `done menu hidden on start`() {
        initViewModel()
        assertThat(requireNotNull(viewModel.uiState.value).doneMenuVisible).isEqualTo(false)
    }

    @Test
    fun `done menu hidden when ui loading`() {
        initViewModel()
        viewModel.onLoadingProgress(true)
        assertThat(requireNotNull(viewModel.uiState.value).doneMenuVisible).isEqualTo(false)
    }

    @Test
    fun `done menu visible when ui loaded`() {
        initViewModel()
        viewModel.onLoadingProgress(false)
        assertThat(requireNotNull(viewModel.uiState.value).doneMenuVisible).isEqualTo(true)
    }

    @Test
    fun `crop and save image start action triggered on done menu click`() {
        initViewModel()
        viewModel.onDoneMenuClicked()
        assertThat(requireNotNull(viewModel.cropAndSaveImageStateEvent.value).peekContent())
                .isInstanceOf(ImageCropAndSaveStartState::class.java)
    }

    @Test
    fun `navigate back action triggered with success result code on crop success`() {
        viewModel.onCropFinish(cropSuccessResultCode, cropResultData)
        assertThat(requireNotNull(viewModel.navigateBackWithCropResult.value).resultCode)
                .isEqualTo(cropSuccessResultCode)
    }

    @Test
    fun `navigate back action not triggered on crop failure`() {
        viewModel.onCropFinish(cropFailedResultCode, cropResultData)
        assertNull(viewModel.navigateBackWithCropResult.value)
    }

    @Test
    fun `crop and save image success action triggered on crop success`() {
        viewModel.onCropFinish(cropSuccessResultCode, cropResultData)
        assertThat(requireNotNull(viewModel.cropAndSaveImageStateEvent.value).peekContent())
                .isInstanceOf(ImageCropAndSaveSuccessState::class.java)
    }

    @Test
    fun `crop and save image failure action triggered on crop failure`() {
        viewModel.onCropFinish(cropFailedResultCode, cropResultData)
        assertThat(requireNotNull(viewModel.cropAndSaveImageStateEvent.value).peekContent())
                .isInstanceOf(ImageCropAndSaveFailedState::class.java)
    }

    private fun initViewModel() = viewModel.start(TEST_INPUT_IMAGE_PATH, TEST_OUTPUT_FILE_EXTENSION, cacheDir)
}
