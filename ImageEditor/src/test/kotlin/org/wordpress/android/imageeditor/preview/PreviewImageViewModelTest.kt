package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.IMAGE_LOAD_IN_PROGRESS

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
    fun `progress bar and image shown on start`() {
        viewModel.start(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL)

        assertThat(viewModel.loadImageFromData.value).matches {
            it?.uiState == IMAGE_LOAD_IN_PROGRESS
            it?.lowResImageUrl == TEST_LOW_RES_IMAGE_URL
            it?.highResImageUrl == TEST_HIGH_RES_IMAGE_URL
        }
    }
}
