package org.wordpress.android.imageeditor.crop

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

    @Before
    fun setUp() {
        viewModel = CropViewModel()
    }

    @Test
    fun `crop and save image action triggered on done menu click`() {
        viewModel.onDoneMenuClicked()
        assertThat(viewModel.shouldCropAndSaveImage.value).isEqualTo(true)
    }

    /*@Test
    fun `file paths written to bundle `() {
        initViewModel()
        viewModel.writeToBundle(arguments)
//        TODO: Mock
//        verify(arguments).putParcelable(any(), argThat {  })
    }*/

    private fun initViewModel() = viewModel.start(TEST_INPUT_IMAGE_PATH, cacheDir)
}
