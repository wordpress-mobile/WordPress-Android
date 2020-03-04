package org.wordpress.android.imageeditor.crop

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule

class CropViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    // Class under test
    private lateinit var viewModel: CropViewModel

    private val cropResultCode = -1
    private val cropData = Intent()

    @Before
    fun setUp() {
        viewModel = CropViewModel()
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
}
