package org.wordpress.android.imageeditor.crop

import android.os.Bundle
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import java.io.File

private const val TEST_INPUT_IMAGE_PATH = "/input/file/path"
class CropViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()
    private val cacheDir = File("/cache/dir")

    // Class under test
    private lateinit var viewModel: CropViewModel

    private val cropResultCode = -1
    private val cropData = Intent()

    @Before
    fun setUp() {
        viewModel = CropViewModel()
    }

    @Test
    fun `crop screen shown with bundle on start`() {
        initViewModel()
        assertThat(requireNotNull(viewModel.showCropScreenWithBundleEvent.value).peekContent())
                .isInstanceOf(Bundle::class.java)
    }

    @Test
    fun `crop and save image action triggered on done menu click`() {
        viewModel.onDoneMenuClicked()
        assertThat(viewModel.shouldCropAndSaveImage.value).isEqualTo(true)
    }

    @Test
    fun `navigate back action triggered on crop finish`() {
        viewModel.onCropFinish(cropResultCode, cropData)
        assertThat(requireNotNull(viewModel.navigateBackWithCropResult.value).first)
                .isEqualTo(cropResultCode)
        assertThat(requireNotNull(viewModel.navigateBackWithCropResult.value).second)
                .isEqualTo(cropData)
    }

    private fun initViewModel() = viewModel.start(TEST_INPUT_IMAGE_PATH, cacheDir)
}
