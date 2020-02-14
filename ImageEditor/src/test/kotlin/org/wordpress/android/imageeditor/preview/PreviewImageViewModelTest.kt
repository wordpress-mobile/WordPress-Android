package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInitialContentUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageLoadSuccessUiState

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
    fun `progress bar shown on start`() {
        initViewModel()
        assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(true)
    }

    @Test
    fun `initial content shown on start`() {
        initViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(ImageInitialContentUiState::class.java)
    }

    @Test
    fun `success ui displayed on image load success`() {
        initViewModel()
        viewModel.onImageLoadSuccess()
        assertThat(viewModel.uiState.value).isInstanceOf(ImageLoadSuccessUiState::class.java)
    }

    @Test
    fun `failure ui displayed on image load failed`() {
        initViewModel()
        viewModel.onImageLoadFailed()
        assertThat(viewModel.uiState.value).isInstanceOf(ImageLoadFailedUiState::class.java)
    }

    private fun initViewModel() {
        viewModel.onCreateView(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL)
    }
}
