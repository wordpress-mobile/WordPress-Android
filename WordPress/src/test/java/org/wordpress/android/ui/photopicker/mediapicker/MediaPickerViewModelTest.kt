package org.wordpress.android.ui.photopicker.mediapicker

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.photopicker.PermissionsHandler
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.DomainModel
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.photopicker.mediapicker.MediaType.VIDEO
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerViewModel.ActionModeUiModel
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerViewModel.MediaPickerUiState
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerViewModel.PhotoListUiModel
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerViewModel.SoftAskViewUiModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

class MediaPickerViewModelTest : BaseUnitTest() {
    @Mock lateinit var mediaLoaderFactory: MediaLoaderFactory
    @Mock lateinit var mediaLoader: MediaLoader
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var uriWrapper1: UriWrapper
    @Mock lateinit var uriWrapper2: UriWrapper
    @Mock lateinit var permissionsHandler: PermissionsHandler
    @Mock lateinit var context: Context
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: MediaPickerViewModel
    private var uiStates = mutableListOf<MediaPickerUiState>()
    private var navigateEvents = mutableListOf<Event<UriWrapper>>()
    private val singleSelectMediaPickerSetup = MediaPickerSetup(DEVICE, false, setOf(IMAGE), false)
    private val multiSelectMediaPickerSetup = MediaPickerSetup(DEVICE, true, setOf(IMAGE, VIDEO), false)
    private val site = SiteModel()
    private lateinit var firstItem: MediaItem
    private lateinit var secondItem: MediaItem
    private lateinit var videoItem: MediaItem

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = MediaPickerViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                mediaLoaderFactory,
                analyticsUtilsWrapper,
                analyticsTrackerWrapper,
                permissionsHandler,
                context,
                resourceProvider
        )
        uiStates.clear()
        firstItem = MediaItem(1, uriWrapper1)
        secondItem = MediaItem(2, uriWrapper2)
        videoItem = MediaItem(3, uriWrapper1, isVideo = true)
    }

    @Test
    fun `loads data on refresh`() = test {
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertDataList(singleSelectMediaPickerSetup, selectedItems = listOf(), domainItems = listOf(firstItem))
        assertActionModeHidden()
    }

    @Test
    fun `selects single item with single selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectMediaPickerSetup)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
        assertActionModeHidden()

        selectItem(0)

        assertThat(uiStates).hasSize(3)
        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_photo))
    }

    @Test
    fun `clears selection`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectMediaPickerSetup)

        viewModel.refreshData(false)

        selectItem(0)

        viewModel.clearSelection()

        assertThat(uiStates).hasSize(4)

        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `unselects first item when second item selected with single selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectMediaPickerSetup)

        viewModel.refreshData(false)

        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(0)

        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(1)

        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(secondItem),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `selects two items with multi selection available`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem, secondItem), multiSelectMediaPickerSetup)

        viewModel.refreshData(false)

        assertDataList(
                multiSelectMediaPickerSetup,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(1)

        assertDataList(
                multiSelectMediaPickerSetup,
                selectedItems = listOf(secondItem),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(0)

        assertDataList(
                multiSelectMediaPickerSetup,
                selectedItems = listOf(secondItem, firstItem),
                domainItems = listOf(firstItem, secondItem)
        )

        selectItem(1)

        assertDataList(
                multiSelectMediaPickerSetup,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `navigates to preview on item click`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectMediaPickerSetup)
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
        clickItem(0)
        assertThat(navigateEvents).isNotEmpty
        verify(analyticsTrackerWrapper).track(eq(MEDIA_PICKER_PREVIEW_OPENED), any<MutableMap<String, Any>>())
    }

    @Test
    fun `shows soft ask screen when storage permissions are turned off`() = test {
        setupViewModel(listOf(), singleSelectMediaPickerSetup, hasStoragePermissions = false)
        whenever(resourceProvider.getString(R.string.app_name)).thenReturn("WordPress")
        whenever(resourceProvider.getString(R.string.photo_picker_soft_ask_label)).thenReturn("Soft ask label")

        viewModel.checkStoragePermission(isAlwaysDenied = false)

        assertThat(uiStates).hasSize(3)

        assertSoftAskUiModelVisible()
    }

    @Test
    fun `action mode title is Use Photo when photo browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), MediaPickerSetup(DEVICE, false, setOf(IMAGE), false))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_photo))
    }

    @Test
    fun `action mode title is Use Video when video browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), MediaPickerSetup(DEVICE, false, setOf(VIDEO), false))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_video), showEditAction = false)
    }

    @Test
    fun `action mode title is Use Media when image and video browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), MediaPickerSetup(DEVICE, false, setOf(IMAGE, VIDEO), false))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_media))
    }

    @Test
    fun `action mode title is Select N items when multi selection available`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem, secondItem), MediaPickerSetup(DEVICE, true, setOf(IMAGE), false))

        viewModel.refreshData(false)

        selectItem(0)
        selectItem(1)

        assertActionModeVisible(UiStringText("2 selected"))
    }

    @Test
    fun `action mode hides edit action when video item selected`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(videoItem, secondItem), MediaPickerSetup(DEVICE, true, setOf(IMAGE, VIDEO), false))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringText("1 selected"), showEditAction = false)
    }

    private fun selectItem(position: Int) {
        (uiStates.last().photoListUiModel as PhotoListUiModel.Data).items[position].toggleAction.toggle()
    }

    private fun clickItem(position: Int) {
        (uiStates.last().photoListUiModel as PhotoListUiModel.Data).items[position].clickAction.click()
    }

    private fun assertDataList(
        mediaPickerSetup: MediaPickerSetup,
        selectedItems: List<MediaItem>,
        domainItems: List<MediaItem>
    ) {
        uiStates.last().apply {
            assertThat(this.photoListUiModel).isNotNull()
            (uiStates.last().photoListUiModel as PhotoListUiModel.Data).apply {
                assertThat(this.items).hasSize(domainItems.size)
                domainItems.forEachIndexed { index, photoPickerItem ->
                    val isSelected = selectedItems.any { it.id == photoPickerItem.id }
                    assertSelection(
                            position = index,
                            isSelected = isSelected,
                            domainItem = photoPickerItem,
                            selectedOrder = selectedItems.indexOfFirst { it.id == photoPickerItem.id },
                            isMultiSelection = mediaPickerSetup.canMultiselect
                    )
                }
            }
        }
        assertThat(viewModel.numSelected()).isEqualTo(selectedItems.size)
        assertThat(viewModel.selectedURIs()).isEqualTo(selectedItems.map { it.uri })
        assertSoftAskUiModelHidden()
    }

    private fun assertSoftAskUiModelVisible() {
        uiStates.last().softAskViewUiModel.let {
            val model = it as SoftAskViewUiModel.Visible
            assertThat(model.allowId).isEqualTo(UiStringRes(R.string.photo_picker_soft_ask_allow))
            assertThat(model.isAlwaysDenied).isEqualTo(false)
            assertThat(model.label).isEqualTo("Soft ask label")
        }
    }

    private fun assertSoftAskUiModelHidden() {
        uiStates.last().softAskViewUiModel.let {
            assertThat(it is SoftAskViewUiModel.Hidden).isTrue()
        }
    }

    private suspend fun setupViewModel(
        domainModel: List<MediaItem>,
        mediaPickerSetup: MediaPickerSetup,
        hasStoragePermissions: Boolean = true
    ) {
        whenever(permissionsHandler.hasStoragePermission()).thenReturn(hasStoragePermissions)
        whenever(mediaLoaderFactory.build(mediaPickerSetup.dataSource)).thenReturn(mediaLoader)
        whenever(mediaLoader.loadMedia(any())).thenReturn(flow { emit(DomainModel(domainModel)) })
        viewModel.start(listOf(), mediaPickerSetup, null, site)
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
        assertThat(uiStates).hasSize(2)
    }

    private fun PhotoListUiModel.Data.assertSelection(
        position: Int,
        isSelected: Boolean,
        isMultiSelection: Boolean = false,
        selectedOrder: Int,
        domainItem: MediaItem
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

    private fun MediaPickerUiItem.assertEqualToDomainItem(domainItem: MediaItem) {
        assertThat(this.id).isEqualTo(domainItem.id)
        if (domainItem.isVideo) {
            assertThat(this is MediaPickerUiItem.VideoItem)
        } else {
            assertThat(this is MediaPickerUiItem.PhotoItem)
        }

        assertThat(this.uri).isEqualTo(domainItem.uri)
    }

    private fun assertActionModeHidden() {
        uiStates.last().actionModeUiModel.let { model ->
            assertThat(model is ActionModeUiModel.Hidden).isTrue()
        }
    }

    private fun assertActionModeVisible(title: UiString, showEditAction: Boolean = true) {
        uiStates.last().actionModeUiModel.let {
            val model = it as ActionModeUiModel.Visible
            assertThat(model.actionModeTitle).isEqualTo(title)
            assertThat(model.showEditAction).isEqualTo(showEditAction)
        }
    }
}
