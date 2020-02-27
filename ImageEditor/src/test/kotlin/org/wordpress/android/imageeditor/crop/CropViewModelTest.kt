package org.wordpress.android.imageeditor.crop

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

    @Before
    fun setUp() {
        viewModel = CropViewModel()
    }

    @Test
    fun `crop and save image action triggered on done menu click`() {
        viewModel.onDoneMenuClicked()
        assertThat(viewModel.shouldCropAndSaveImage.value).isEqualTo(true)
    }
}
