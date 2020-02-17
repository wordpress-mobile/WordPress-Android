package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState

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
    fun `data loading started on view create`() {
        initViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(ImageDataStartLoadingUiState::class.java)
    }

    @Test
    fun `progress bar shown on view create`() {
        initViewModel()
        assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(true)
    }

    @Test
    fun `progress bar shown on low res image load success`() {
        initViewModel()
        viewModel.onImageLoadSuccess(TEST_LOW_RES_IMAGE_URL)
        assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(true)
    }

    @Test
    fun `progress bar hidden on low res image load failed`() {
        initViewModel()
        viewModel.onImageLoadFailed(TEST_LOW_RES_IMAGE_URL)
        assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(false)
    }

    @Test
    fun `low res image success ui shown on low res image load success`() {
        initViewModel()
        viewModel.onImageLoadSuccess(TEST_LOW_RES_IMAGE_URL)
        assertThat(viewModel.uiState.value).isInstanceOf(ImageInLowResLoadSuccessUiState::class.java)
    }

    @Test
    fun `low res image failed ui shown on low res image load failed`() {
        initViewModel()
        viewModel.onImageLoadFailed(TEST_LOW_RES_IMAGE_URL)
        assertThat(viewModel.uiState.value).isInstanceOf(ImageInLowResLoadFailedUiState::class.java)
    }

    private fun initViewModel() = viewModel.onCreateView(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL)
}
