package org.wordpress.android.imageeditor.preview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.imageeditor.R
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.InputData
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.OutputData
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileFailedState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileIdleState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageLoadToFileSuccessState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageLoadToFileState.ImageStartLoadingToFileState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageDataStartLoadingUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInHighResLoadSuccessUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadFailedUiState
import org.wordpress.android.imageeditor.preview.PreviewImageViewModel.ImageUiState.ImageInLowResLoadSuccessUiState
import java.net.URI

private const val TEST_LOW_RES_IMAGE_URL = "https://wordpress.com/low_res_image.png"
private const val TEST_HIGH_RES_IMAGE_URL = "https://wordpress.com/image.png"
private const val TEST_OUTPUT_FILE_EXTENSION = ".png"
private const val TEST_INPUT_FILE_PATH_TO_CROP = "/file/path/to/crop"

private const val TEST2_LOW_RES_IMAGE_URL = "https://wordpress.com/low_res_image2.jpg"
private const val TEST2_HIGH_RES_IMAGE_URL = "https://wordpress.com/image2.jpg"
private const val TEST2_OUTPUT_FILE_EXTENSION = ".jpg"
private const val TEST2_OUTPUT_FILE_PATH_FROM_CROP = "file://path/from/crop/2"
private const val FIRST_ITEM_POSITION = 0
private const val SECOND_ITEM_POSITION = 1

private const val TEST_ERROR = "Error"

class PreviewImageViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    // Class under test
    private lateinit var viewModel: PreviewImageViewModel

    private val testInputDataList = listOf(
        InputData(TEST_HIGH_RES_IMAGE_URL, TEST_LOW_RES_IMAGE_URL, TEST_OUTPUT_FILE_EXTENSION),
        InputData(TEST2_HIGH_RES_IMAGE_URL, TEST2_LOW_RES_IMAGE_URL, TEST2_OUTPUT_FILE_EXTENSION)
    )

    @Before
    fun setUp() {
        viewModel = PreviewImageViewModel()
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
        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates.first().progressBarVisible
        ).isEqualTo(true)
    }

    @Test
    fun `thumbnails tab layout not visible if single img available`() {
        initViewModel(testInputDataList.subList(0, 1))
        assertThat(requireNotNull(viewModel.uiState.value).thumbnailsTabLayoutVisible).isEqualTo(false)
    }

    @Test
    fun `thumbnails tab layout visible if multiple images available`() {
        initViewModel()

        assertThat(testInputDataList.size).isGreaterThan(1)
        assertThat(requireNotNull(viewModel.uiState.value).thumbnailsTabLayoutVisible).isEqualTo(true)
    }

    @Test
    fun `progress bar shown on low res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].progressBarVisible
        ).isEqualTo(true)
    }

    @Test
    fun `progress bar shown on low res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].progressBarVisible
        ).isEqualTo(true)
    }

    @Test
    fun `low res image success ui shown on low res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
            .isInstanceOf(ImageInLowResLoadSuccessUiState::class.java)
    }

    @Test
    fun `low res image failed ui shown on low res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
            .isInstanceOf(ImageInLowResLoadFailedUiState::class.java)
    }

    @Test
    fun `progress bar hidden on high res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].progressBarVisible
        ).isEqualTo(false)
    }

    @Test
    fun `progress bar hidden on high res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].progressBarVisible
        ).isEqualTo(false)
    }

    @Test
    fun `high res image success ui shown on high res image load success at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
            .isInstanceOf(ImageInHighResLoadSuccessUiState::class.java)
    }

    @Test
    fun `high res image failed ui shown on high res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
            .isInstanceOf(ImageInHighResLoadFailedUiState::class.java)
    }

    @Test
    fun `high res image success ui shown when low res image loads after high res image at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
            .isInstanceOf(ImageInHighResLoadSuccessUiState::class.java)
    }

    @Test
    fun `high res image auto file loading not started when high res image shown and multiple images available`() {
        initViewModel()

        assertThat(testInputDataList.size).isGreaterThan(1)

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(ImageLoadToFileIdleState)
    }

    @Test
    fun `high res image auto file loading started when high res image shown and single img available`() {
        initViewModel(testInputDataList.subList(0, 1))

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(
            ImageStartLoadingToFileState(TEST_HIGH_RES_IMAGE_URL, itemPosition)
        )
    }

    @Test
    fun `high res img auto file loading started when low res img shown after high res img and single img available`() {
        initViewModel(testInputDataList.subList(0, 1))

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, itemPosition)
        viewModel.onLoadIntoImageViewSuccess(TEST_LOW_RES_IMAGE_URL, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(
            ImageStartLoadingToFileState(
                TEST_HIGH_RES_IMAGE_URL, itemPosition
            )
        )
    }

    @Test
    fun `high res img file load not started when low res img shown, high res img show failed and single img avail`() {
        initViewModel(testInputDataList.subList(0, 1))

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(TEST_LOW_RES_IMAGE_URL, itemPosition)
        viewModel.onLoadIntoImageViewFailed(TEST_HIGH_RES_IMAGE_URL, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(ImageLoadToFileIdleState)
    }

    @Test
    fun `high res img file loading started when low res img show failed, high res img shown and single img avail`() {
        initViewModel(testInputDataList.subList(0, 1))

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(TEST_LOW_RES_IMAGE_URL, itemPosition)
        viewModel.onLoadIntoImageViewSuccess(TEST_HIGH_RES_IMAGE_URL, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(
            ImageStartLoadingToFileState(TEST_HIGH_RES_IMAGE_URL, itemPosition)
        )
    }

    @Test
    fun `load image to file success state triggered for the loaded img file on image loaded`() {
        initViewModel()

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoFileSuccess(TEST_INPUT_FILE_PATH_TO_CROP, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent())
            .isEqualTo(ImageLoadToFileSuccessState(TEST_INPUT_FILE_PATH_TO_CROP, itemPosition))
    }

    @Test
    fun `load image to file success state triggered on image load to file success`() {
        initViewModel()

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoFileSuccess(TEST_INPUT_FILE_PATH_TO_CROP, itemPosition)

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent())
            .isInstanceOf(ImageLoadToFileSuccessState::class.java)
    }

    @Test
    fun `load image to file failed state triggered on image load to file failure`() {
        initViewModel()
        val exception = Exception(TEST_ERROR)
        viewModel.onLoadIntoFileFailed(exception)

        val loadFileState = requireNotNull(viewModel.loadIntoFile.value).peekContent()
        assertThat(loadFileState)
            .isEqualTo(ImageLoadToFileFailedState(exception.message, R.string.error_failed_to_load_into_file))
    }

    @Test
    fun `navigated to crop screen with file info on image load to file success and multiple imgs available`() {
        initViewModel()

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoFileSuccess(TEST_INPUT_FILE_PATH_TO_CROP, itemPosition)

        val (inputFile, fileExtn, shouldReturnToPreviewScreen) =
            requireNotNull(viewModel.navigateToCropScreenWithFileInfo.value).peekContent()

        assertThat(inputFile).isEqualTo(TEST_INPUT_FILE_PATH_TO_CROP)
        assertThat(fileExtn).isEqualTo(TEST_OUTPUT_FILE_EXTENSION)
        assertThat(shouldReturnToPreviewScreen).isEqualTo(true)
    }

    @Test
    fun `navigated to crop screen with file info on image load to file success and single img available`() {
        initViewModel(testInputDataList.subList(0, 1))

        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoFileSuccess(TEST_INPUT_FILE_PATH_TO_CROP, itemPosition)

        val (inputFile, fileExtn, shouldReturnToPreviewScreen) =
            requireNotNull(viewModel.navigateToCropScreenWithFileInfo.value).peekContent()
        assertThat(inputFile).isEqualTo(TEST_INPUT_FILE_PATH_TO_CROP)
        assertThat(fileExtn).isEqualTo(TEST_OUTPUT_FILE_EXTENSION)
        assertThat(shouldReturnToPreviewScreen).isEqualTo(false)
    }

    @Test
    fun `not navigated to crop screen on image load to file failure`() {
        initViewModel()
        val exception = Exception(TEST_ERROR)
        viewModel.onLoadIntoFileFailed(exception)
        assertNull(viewModel.navigateToCropScreenWithFileInfo.value)
    }

    @Test
    fun `retry layout shown on high res image load failed at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].retryLayoutVisible
        ).isEqualTo(true)
    }

    @Test
    fun `retry layout shown when low res image load succeeded but high res image load failed at given item pos`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].retryLayoutVisible
        ).isEqualTo(true)
    }

    @Test
    fun `retry layout hidden when low res image load failed but high res image shown at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].retryLayoutVisible
        ).isEqualTo(false)
    }

    @Test
    fun `data loading start triggered on retry at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)
        requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition].onItemTapped?.invoke()

        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition])
            .isInstanceOf(ImageDataStartLoadingUiState::class.java)
    }

    @Test
    fun `progress bar not visible when retry layout is visible and low res image loaded at given item position`() {
        initViewModel()
        val itemPosition = FIRST_ITEM_POSITION
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .retryLayoutVisible
        ).isEqualTo(true)

        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_LOW_RES_IMAGE_URL, position = itemPosition)

        assertThat(
            requireNotNull(viewModel.uiState.value).viewPagerItemsStates[itemPosition]
                .progressBarVisible
        ).isEqualTo(false)
    }

    @Test
    fun `edit actions enabled if high res image loaded at selected page position`() {
        initViewModel()

        val selectedPosition = SECOND_ITEM_POSITION

        viewModel.onPageSelected(selectedPosition)
        viewModel.onLoadIntoImageViewSuccess(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = selectedPosition)

        assertThat(requireNotNull(viewModel.uiState.value).editActionsEnabled).isEqualTo(true)
    }

    @Test
    fun `edit actions not enabled if high res image not loaded at selected page pos but loaded at another pos`() {
        initViewModel()

        val selectedPosition = FIRST_ITEM_POSITION

        viewModel.onPageSelected(selectedPosition)
        viewModel.onLoadIntoImageViewFailed(imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL, position = selectedPosition)
        viewModel.onLoadIntoImageViewSuccess(
            imageUrlAtPosition = TEST_HIGH_RES_IMAGE_URL,
            position = SECOND_ITEM_POSITION
        )

        assertThat(requireNotNull(viewModel.uiState.value).editActionsEnabled).isEqualTo(false)
    }

    @Test
    fun `high res image file loading started when crop action triggered and high res url is not a file url`() {
        initViewModel(testInputDataList)

        val selectedPosition = SECOND_ITEM_POSITION
        viewModel.onPageSelected(selectedPosition)

        viewModel.onCropMenuClicked()

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(
            ImageStartLoadingToFileState(testInputDataList.get(selectedPosition).highResImgUrl, selectedPosition)
        )
    }

    @Test
    fun `high res image file loading skipped when crop action triggered and high res url is a file url`() {
        initViewModel(
            listOf(
                InputData(
                    TEST2_OUTPUT_FILE_PATH_FROM_CROP,
                    TEST2_OUTPUT_FILE_PATH_FROM_CROP,
                    TEST2_OUTPUT_FILE_EXTENSION
                )
            )
        )

        val selectedPosition = FIRST_ITEM_POSITION
        viewModel.onPageSelected(selectedPosition)

        viewModel.onCropMenuClicked()

        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent())
            .isNotInstanceOf(ImageStartLoadingToFileState::class.java)
        assertThat(requireNotNull(viewModel.loadIntoFile.value).peekContent()).isEqualTo(
            ImageLoadToFileSuccessState(URI(TEST2_OUTPUT_FILE_PATH_FROM_CROP).path, selectedPosition)
        )
    }

    @Test
    fun `thumbnail and preview image view loading starts with cropped file path on crop result`() {
        // when - selected position is set in the viewmodel
        initViewModel()
        val selectedPosition = SECOND_ITEM_POSITION
        viewModel.onPageSelected(selectedPosition)

        // crop result obtained with outfile file at selected position
        viewModel.onCropResult(TEST2_OUTPUT_FILE_PATH_FROM_CROP)

        // assert that thumbnail and preview image start loading with the output file path for the cropped image
        assertThat(requireNotNull(viewModel.uiState.value).viewPagerItemsStates[selectedPosition]).isInstanceOf(
            ImageDataStartLoadingUiState::class.java
        )

        val imageData = requireNotNull(viewModel.uiState.value).viewPagerItemsStates[selectedPosition].data
        assertThat(imageData.highResImageUrl).isEqualTo(TEST2_OUTPUT_FILE_PATH_FROM_CROP)
        assertThat(imageData.lowResImageUrl).isEqualTo(TEST2_OUTPUT_FILE_PATH_FROM_CROP)
    }

    @Test
    fun `output data list on insert click is a list of high res image urls mapped from input data list`() {
        initViewModel(testInputDataList)
        viewModel.onInsertClicked()

        assertThat(requireNotNull(viewModel.finishAction.value).peekContent()).isEqualTo(
            testInputDataList.map { OutputData(it.highResImgUrl) }
        )
    }

    @Test
    fun `number of images count is equal to input image list size`() {
        initViewModel(testInputDataList)
        assertThat(viewModel.numberOfImages).isEqualTo(testInputDataList.size)
    }

    private fun initViewModel(
        inputDataList: List<InputData> = testInputDataList
    ) = viewModel.onCreateView(inputDataList, mock())
}
