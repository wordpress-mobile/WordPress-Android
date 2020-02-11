package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule

private const val TEST_IMAGE_URL = "https://developer.android.com/images/about/versions/10/android10_black.png"
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
        viewModel.start(TEST_IMAGE_URL)
        assertThat(viewModel.loadImageFromUrl.value).isEqualTo(TEST_IMAGE_URL)
    }
}
