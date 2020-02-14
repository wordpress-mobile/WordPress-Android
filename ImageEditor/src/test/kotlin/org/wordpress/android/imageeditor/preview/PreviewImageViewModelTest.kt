package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadInProgressUiState

private const val TEST_LOW_RES_IMAGE_URL = "https://wordpress.com/low_res_image.png"
private const val TEST_HIGH_RES_IMAGE_URL = "https://wordpress.com/image.png"
class PreviewImageViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    // Class under test
    private lateinit var viewModel: PreviewImageViewModel

    @Before
    fun setUp() {
        viewModel = PreviewImageViewModel()
    }

    @Test
    fun `low and high res image shown on start`() {
        initViewModel()
        assertThat(viewModel.loadImageFromData.value).matches {
            it?.lowResImageUrl == TEST_LOW_RES_IMAGE_URL
            it?.highResImageUrl == TEST_HIGH_RES_IMAGE_URL
        }
    }

    @Test
    fun `progress bar shown on start`() {
        initViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(ImageLoadInProgressUiState::class.java)
    }

    private fun initViewModel() {
        viewModel.start(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL)
    }
}
