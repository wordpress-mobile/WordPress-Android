package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule

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
    fun `image shown on start`() {
        viewModel.start(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL)
        assertThat(viewModel.loadImageFromData.value?.lowResImageUrl)?.isEqualTo(TEST_LOW_RES_IMAGE_URL)
        assertThat(viewModel.loadImageFromData.value?.highResImageUrl)?.isEqualTo(TEST_HIGH_RES_IMAGE_URL)
    }
}
