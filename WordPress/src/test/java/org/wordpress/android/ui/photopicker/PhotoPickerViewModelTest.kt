package org.wordpress.android.ui.photopicker

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class PhotoPickerViewModelTest : BaseUnitTest() {
    @Suppress("DEPRECATION")
    @Mock
    lateinit var deviceMediaListBuilder: DeviceMediaListBuilder

    @Mock
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var uriWrapper1: UriWrapper

    @Mock
    lateinit var uriWrapper2: UriWrapper

    @Mock
    lateinit var permissionsHandler: PermissionsHandler

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var copyMediaToAppStorageUseCase: CopyMediaToAppStorageUseCase

    @Mock
    lateinit var getMediaModelUseCase: GetMediaModelUseCase

    @Suppress("DEPRECATION")
    private lateinit var viewModel: PhotoPickerViewModel

    @Suppress("DEPRECATION")
    private var uiStates = mutableListOf<PhotoPickerViewModel.PhotoPickerUiState>()
    private var navigateEvents = mutableListOf<Event<UriWrapper>>()
    private val singleSelectBrowserType = MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER
    private val multiSelectBrowserType = MediaBrowserType.GUTENBERG_IMAGE_PICKER
    private val site = SiteModel()

    @Suppress("DEPRECATION")
    private lateinit var firstItem: PhotoPickerItem

    @Suppress("DEPRECATION")
    private lateinit var secondItem: PhotoPickerItem

    @Before
    @Suppress("DEPRECATION")
    fun setUp() {
        viewModel = PhotoPickerViewModel(
            testDispatcher(),
            testDispatcher(),
            deviceMediaListBuilder,
            analyticsUtilsWrapper,
            analyticsTrackerWrapper,
            permissionsHandler,
            resourceProvider,
            copyMediaToAppStorageUseCase,
            getMediaModelUseCase
        )
        uiStates.clear()
        firstItem = PhotoPickerItem(1, uriWrapper1, false)
        secondItem = PhotoPickerItem(2, uriWrapper2, false)
    }

    @Test
    fun `loads data on refresh`() = test {
        setupViewModel(listOf(firstItem), singleSelectBrowserType)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertDataList(singleSelectBrowserType, selectedItems = listOf(), domainItems = listOf(firstItem))
        assertSingleIconMediaBottomBarVisible()
        assertActionModeHidden()
    }

    @Test
    fun `selects single item with single selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertDataList(
            singleSelectBrowserType,
            selectedItems = listOf(),
            domainItems = listOf(firstItem, secondItem)
        )
        assertSingleIconMediaBottomBarVisible()
        assertActionModeHidden()

        selectItem(0)

        assertThat(uiStates).hasSize(3)
        assertDataList(
            singleSelectBrowserType,
            selectedItems = listOf(firstItem),
            domainItems = listOf(firstItem, secondItem)
        )
        assertInsertEditBottomBarVisible()
        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_photo))
    }

    @Test
    fun `clears selection`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)

        viewModel.refreshData(false)

        selectItem(0)

        assertInsertEditBottomBarVisible()

        viewModel.clearSelection()

        assertThat(uiStates).hasSize(4)

        assertDataList(
            singleSelectBrowserType,
            selectedItems = listOf(),
            domainItems = listOf(firstItem, secondItem)
        )
        assertSingleIconMediaBottomBarVisible()
    }

    @Test
    fun `unselects first item when second item selected with single selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)

        viewModel.refreshData(false)

        assertDataList(
            singleSelectBrowserType,
            selectedItems = listOf(),
            domainItems = listOf(firstItem, secondItem)
        )
        selectItem(0)

        assertDataList(
            singleSelectBrowserType,
            selectedItems = listOf(firstItem),
            domainItems = listOf(firstItem, secondItem)
        )
        selectItem(1)

        assertDataList(
            singleSelectBrowserType,
            selectedItems = listOf(secondItem),
            domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `selects two items with multi selection available`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem, secondItem), multiSelectBrowserType)

        viewModel.refreshData(false)

        assertDataList(
            multiSelectBrowserType,
            selectedItems = listOf(),
            domainItems = listOf(firstItem, secondItem)
        )
        assertSingleIconMediaBottomBarVisible()
        selectItem(1)

        assertDataList(
            multiSelectBrowserType,
            selectedItems = listOf(secondItem),
            domainItems = listOf(firstItem, secondItem)
        )
        assertInsertEditBottomBarVisible()
        selectItem(0)

        assertDataList(
            multiSelectBrowserType,
            selectedItems = listOf(secondItem, firstItem),
            domainItems = listOf(firstItem, secondItem)
        )
        assertInsertEditBottomBarVisible()

        selectItem(1)

        assertDataList(
            multiSelectBrowserType,
            selectedItems = listOf(firstItem),
            domainItems = listOf(firstItem, secondItem)
        )
        assertInsertEditBottomBarVisible()
    }

    @Test
    fun `navigates to preview on item click`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)
        whenever(
            analyticsUtilsWrapper.getMediaProperties(
                eq(firstItem.isVideo),
                eq(firstItem.uri),
                isNull()
            )
        ).thenReturn(
            mutableMapOf()
        )

        viewModel.refreshData(false)

        assertThat(navigateEvents).isEmpty()
        clickItem()
        assertThat(navigateEvents).isNotEmpty
        verify(analyticsTrackerWrapper).track(eq(MEDIA_PICKER_PREVIEW_OPENED), any<MutableMap<String, Any>>())
    }

    @Test
    fun `shows soft ask screen when photos videos permissions are turned off`() = test {
        setupViewModel(listOf(), singleSelectBrowserType, hasPhotosVideosPermissions = false)
        whenever(resourceProvider.getString(R.string.app_name)).thenReturn("WordPress")
        whenever(resourceProvider.getString(R.string.photo_picker_soft_ask_photos_label)).thenReturn("Soft ask label")

        viewModel.checkMediaPermissions(isPhotosVideosAlwaysDenied = false, isMusicAudioAlwaysDenied = false)

        assertThat(uiStates).hasSize(2)

        assertSoftAskUiModelVisible()
        assertBottomBarHidden()
    }

    @Test
    fun `action mode title is Use Photo when photo browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER)

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_photo))
    }

    @Test
    fun `action mode title is Use Video when video browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER)

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_video))
    }

    @Test
    fun `action mode title is Use Media when image and video browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), MediaBrowserType.GUTENBERG_MEDIA_PICKER)

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_media))
    }

    @Test
    fun `action mode title is Select N items when multi selection available`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem, secondItem), MediaBrowserType.GUTENBERG_IMAGE_PICKER)

        viewModel.refreshData(false)

        selectItem(0)
        selectItem(1)

        assertActionModeVisible(UiStringText("2 selected"))
    }

    @Test
    fun `action mode shows confirmation action in EDITOR PICKER`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem, secondItem), MediaBrowserType.EDITOR_PICKER)

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringText("1 selected"), showConfirmationAction = true)
    }

    @Suppress("DEPRECATION")
    private fun selectItem(position: Int) {
        (uiStates.last().photoListUiModel as PhotoPickerViewModel.PhotoListUiModel.Data)
            .items[position]
            .toggleAction
            .toggle()
    }

    @Suppress("DEPRECATION")
    private fun clickItem() {
        (uiStates.last().photoListUiModel as PhotoPickerViewModel.PhotoListUiModel.Data)
            .items[0]
            .clickAction
            .click()
    }

    @Suppress("DEPRECATION")
    private fun assertDataList(
        browserType: MediaBrowserType,
        selectedItems: List<PhotoPickerItem>,
        domainItems: List<PhotoPickerItem>
    ) {
        uiStates.last().apply {
            assertThat(this.photoListUiModel).isNotNull
            (uiStates.last().photoListUiModel as PhotoPickerViewModel.PhotoListUiModel.Data).apply {
                assertThat(this.items).hasSize(domainItems.size)
                domainItems.forEachIndexed { index, photoPickerItem ->
                    val isSelected = selectedItems.any { it.id == photoPickerItem.id }
                    assertSelection(
                        position = index,
                        isSelected = isSelected,
                        domainItem = photoPickerItem,
                        selectedOrder = selectedItems.indexOfFirst { it.id == photoPickerItem.id },
                        isMultiSelection = browserType.canMultiselect()
                    )
                }
            }
        }
        assertThat(viewModel.numSelected()).isEqualTo(selectedItems.size)
        assertThat(viewModel.selectedURIs()).isEqualTo(selectedItems.map { it.uri })
        assertSoftAskUiModelHidden()
    }

    @Suppress("DEPRECATION")
    private fun assertSoftAskUiModelVisible() {
        uiStates.last().softAskViewUiModel.let {
            val model = it as PhotoPickerViewModel.SoftAskViewUiModel.Visible
            assertThat(model.allowId).isEqualTo(UiStringRes(R.string.photo_picker_soft_ask_allow))
            assertThat(model.isAlwaysDenied).isEqualTo(false)
            assertThat(model.label).isEqualTo("Soft ask label")
        }
    }

    @Suppress("DEPRECATION")
    private fun assertSoftAskUiModelHidden() {
        uiStates.last().softAskViewUiModel.let {
            assertThat(it is PhotoPickerViewModel.SoftAskViewUiModel.Hidden).isTrue
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun setupViewModel(
        domainModel: List<PhotoPickerItem>,
        browserType: MediaBrowserType,
        hasPhotosVideosPermissions: Boolean = true
    ) {
        whenever(permissionsHandler.hasPhotosVideosPermission()).thenReturn(hasPhotosVideosPermissions)
        viewModel.start(listOf(), browserType, null, site)
        whenever(deviceMediaListBuilder.buildDeviceMedia(browserType)).thenReturn(domainModel)
        viewModel.uiState.observeForever {
            if (it != null) {
                uiStates.add(it)
            }
        }
        viewModel.onNavigateToPreview.observeForever {
            if (it != null) {
                navigateEvents.add(it)
            }
        }
        assertThat(uiStates).hasSize(1)
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerViewModel.PhotoListUiModel.Data.assertSelection(
        position: Int,
        isSelected: Boolean,
        isMultiSelection: Boolean = false,
        selectedOrder: Int,
        domainItem: PhotoPickerItem
    ) {
        this.items[position].apply {
            assertThat(this.isSelected).isEqualTo(isSelected)
            if (isSelected && isMultiSelection) {
                assertThat(this.selectedOrder).isEqualTo(selectedOrder + 1)
            } else {
                assertThat(this.selectedOrder).isNull()
            }
            assertThat(this.showOrderCounter).isEqualTo(isMultiSelection)
            assertEqualToDomainItem(domainItem)
        }
    }

    @Suppress("DEPRECATION")
    private fun PhotoPickerUiItem.assertEqualToDomainItem(domainItem: PhotoPickerItem) {
        assertThat(this.id).isEqualTo(domainItem.id)
        if (domainItem.isVideo) {
            assertThat(this is PhotoPickerUiItem.VideoItem)
        } else {
            assertThat(this is PhotoPickerUiItem.PhotoItem)
        }

        assertThat(this.uri).isEqualTo(domainItem.uri)
    }

    @Suppress("DEPRECATION")
    private fun assertBottomBarHidden() {
        uiStates.last().apply {
            assertThat(bottomBarUiModel.type).isEqualTo(PhotoPickerViewModel.BottomBarUiModel.BottomBar.NONE)
        }
    }

    @Suppress("DEPRECATION")
    private fun assertSingleIconMediaBottomBarVisible() {
        uiStates.last().apply {
            assertThat(bottomBarUiModel.type).isEqualTo(PhotoPickerViewModel.BottomBarUiModel.BottomBar.MEDIA_SOURCE)
            assertThat(bottomBarUiModel.canShowInsertEditBottomBar).isTrue
            assertThat(bottomBarUiModel.hideMediaBottomBarInPortrait).isFalse
            assertThat(bottomBarUiModel.showCameraButton).isFalse
            assertThat(bottomBarUiModel.showWPMediaIcon).isFalse()
        }
    }

    @Suppress("DEPRECATION")
    private fun assertInsertEditBottomBarVisible() {
        uiStates.last().apply {
            assertThat(bottomBarUiModel.type).isEqualTo(PhotoPickerViewModel.BottomBarUiModel.BottomBar.INSERT_EDIT)
            assertThat(bottomBarUiModel.canShowInsertEditBottomBar).isTrue
            assertThat(bottomBarUiModel.hideMediaBottomBarInPortrait).isFalse
            assertThat(bottomBarUiModel.showCameraButton).isFalse
            assertThat(bottomBarUiModel.showWPMediaIcon).isFalse()
        }
    }

    @Suppress("DEPRECATION")
    private fun assertActionModeHidden() {
        uiStates.last().actionModeUiModel.let { model ->
            assertThat(model is PhotoPickerViewModel.ActionModeUiModel.Hidden).isTrue
        }
    }

    @Suppress("DEPRECATION")
    private fun assertActionModeVisible(title: UiString, showConfirmationAction: Boolean = false) {
        uiStates.last().actionModeUiModel.let {
            val model = it as PhotoPickerViewModel.ActionModeUiModel.Visible
            assertThat(model.actionModeTitle).isEqualTo(title)
            assertThat(model.showConfirmAction).isEqualTo(showConfirmationAction)
        }
    }
}
