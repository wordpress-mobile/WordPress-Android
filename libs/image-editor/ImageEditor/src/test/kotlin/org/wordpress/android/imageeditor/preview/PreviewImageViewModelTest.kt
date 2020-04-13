package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState

private const val TEST_LOW_RES_IMAGE_URL = "https://wordpress.com/low_res_image.png"
private const val TEST_HIGH_RES_IMAGE_URL = "https://wordpress.com/image.png"
// private const val TEST_FILE_PATH = "/file/path"
private const val TEST_OUTPUT_FILE_EXTENSION = ".jpg"
private const val FIRST_ITEM_POSITION = 0

class PreviewImageViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    // Class under test
    private lateinit var viewModel: PreviewImageViewModel

    private lateinit var imageData: List<ImageData>

    @Before
    fun setUp() {
        viewModel = PreviewImageViewModel()
        imageData = listOf(ImageData(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL, TEST_OUTPUT_FILE_EXTENSION))
    }

    @Test
    fun `first item data loading started on view create`() {
        initViewModel()
        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates.first())
                .isInstanceOf(ImageDataStartLoadingUiState::class.java)
    }

    @Test
    fun `first item progress bar shown on view create`() {
        initViewModel()
        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates.first()
                .progressBarVisible).isEqualTo(true)
    }

    @Test
    fun `progress bar shown on low res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .progressBarVisible).isEqualTo(true)
    }

    @Test
    fun `progress bar shown on low res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .progressBarVisible).isEqualTo(true)
    }

    @Test
    fun `low res image success ui shown on low res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
                .isInstanceOf(ImageInLowResLoadSuccessUiState::class.java)
    }

    @Test
    fun `low res image failed ui shown on low res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
                .isInstanceOf(ImageInLowResLoadFailedUiState::class.java)
    }

    @Test
    fun `progress bar hidden on high res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .progressBarVisible).isEqualTo(false)
    }

    @Test
    fun `progress bar hidden on high res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .progressBarVisible).isEqualTo(false)
    }

    @Test
    fun `high res image success ui shown on high res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
                .isInstanceOf(ImageInHighResLoadSuccessUiState::class.java)
    }

    @Test
    fun `high res image failed ui shown on high res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
                .isInstanceOf(ImageInHighResLoadFailedUiState::class.java)
    }

    @Test
    fun `high res image success ui shown when low res image loads after high res image at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
                .isInstanceOf(ImageInHighResLoadSuccessUiState::class.java)
    }

    /*@Test
    fun `high res image file loading started when high res image shown`() {
        initViewModel()
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, imageData)
        assertThat(viewModel.loadIntoFile.value).isEqualTo(ImageStartLoadingToFileState(TEST_HIGH_RES_IMAGE_URL))
    }

    @Test
    fun `high res image file loading started when low res image shown after high res image`() {
        initViewModel()
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, imageData)
        viewModel.onLoadIntoImageViewSuccess(TEST_LOW_RES_IMAGE_URL, imageData)
        assertThat(viewModel.loadIntoFile.value).isEqualTo(ImageStartLoadingToFileState(TEST_HIGH_RES_IMAGE_URL))
    }

    @Test
    fun `high res image file loading not started when low res image shown but high res image show failed`() {
        initViewModel()
        viewModel.onLoadIntoImageViewSuccess(TEST_LOW_RES_IMAGE_URL, imageData)
        viewModel.onLoadIntoImageViewFailed(TEST_HIGH_RES_IMAGE_URL)
        assertThat(viewModel.loadIntoFile.value).isEqualTo(ImageLoadToFileIdleState)
    }

    @Test
    fun `high res image file loading started when low res image show failed but high res image shown`() {
        initViewModel()
        viewModel.onLoadIntoImageViewFailed(TEST_LOW_RES_IMAGE_URL)
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, imageData)
        assertThat(viewModel.loadIntoFile.value).isEqualTo(ImageStartLoadingToFileState(TEST_HIGH_RES_IMAGE_URL))
    }

    @Test
    fun `load image to file success state triggered for the loaded image file on image loaded`() {
        initViewModel()
        viewModel.onLoadIntoFileSuccess(TEST_FILE_PATH)
        assertThat(viewModel.loadIntoFile.value).isEqualTo(ImageLoadToFileSuccessState(TEST_FILE_PATH))
    }

    @Test
    fun `load image to file success state triggered on image load to file success`() {
        initViewModel()
        viewModel.onLoadIntoFileSuccess(TEST_FILE_PATH)
        assertThat(viewModel.loadIntoFile.value).isInstanceOf(ImageLoadToFileSuccessState::class.java)
    }

    @Test
    fun `load image to file failed state triggered on image load to file failure`() {
        initViewModel()
        viewModel.onLoadIntoFileFailed()
        assertThat(viewModel.loadIntoFile.value).isInstanceOf(ImageLoadToFileFailedState::class.java)
    }

    @Test
    fun `navigated to crop screen with file info on image load to file success`() {
        initViewModel()
        viewModel.onLoadIntoFileSuccess(TEST_FILE_PATH)
        assertThat(requireNotNull(viewModel.navigateToCropScreenWithFileInfo.value).first)
                .isEqualTo(TEST_FILE_PATH)
        assertThat(requireNotNull(viewModel.navigateToCropScreenWithFileInfo.value).second)
                .isEqualTo(TEST_OUTPUT_FILE_EXTENSION)
    }

    @Test
    fun `not navigated to crop screen on image load to file failure`() {
        initViewModel()
        viewModel.onLoadIntoFileFailed()
        assertNull(viewModel.navigateToCropScreenWithFileInfo.value)
    }*/

    @Test
    fun `retry layout shown on high res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .retryLayoutVisible).isEqualTo(true)
    }

    @Test
    fun `retry layout shown when low res image load succeeded but high res image load failed at given item pos`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .retryLayoutVisible).isEqualTo(true)
    }

    @Test
    fun `retry layout hidden when low res image load failed but high res image shown at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)
        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .retryLayoutVisible).isEqualTo(false)
    }

    @Test
    fun `data loading start triggered on retry at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)
        requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].onItemTapped?.invoke()

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
                .isInstanceOf(ImageDataStartLoadingUiState::class.java)
    }

    @Test
    fun `progress bar not visible when retry layout is visible and low res image loaded at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(currentUrl = TEST_HIGH_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .retryLayoutVisible).isEqualTo(true)

        viewModel.onLoadIntoImageViewSuccess(currentUrl = TEST_LOW_RES_IMAGE_URL, currentPosition = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .progressBarVisible).isEqualTo(false)
    }

    private fun initViewModel() = viewModel.onCreateView(
        listOf(ImageData(TEST_LOW_RES_IMAGE_URL, TEST_HIGH_RES_IMAGE_URL, TEST_OUTPUT_FILE_EXTENSION))
    )
}
