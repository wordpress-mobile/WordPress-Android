package org.wordpress.android.ui.photopicker

import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.test
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event

class PhotoPickerViewModelTest : BaseUnitTest() {
    @Mock lateinit var deviceMediaListBuilder: DeviceMediaListBuilder
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var uriWrapper: UriWrapper
    private lateinit var viewModel: PhotoPickerViewModel
    private var uiModels = mutableListOf<PhotoPickerUiModel>()
    private var navigateEvents = mutableListOf<Event<UriWrapper>>()
    private val singleSelectBrowserType = GUTENBERG_SINGLE_IMAGE_PICKER
    private val multiSelectBrowserType = MediaBrowserType.GUTENBERG_IMAGE_PICKER
    private lateinit var firstItem: PhotoPickerItem
    private lateinit var secondItem: PhotoPickerItem

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = PhotoPickerViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                deviceMediaListBuilder,
                analyticsUtilsWrapper,
                analyticsTrackerWrapper
        )
        uiModels.clear()
        firstItem = PhotoPickerItem(1, uriWrapper, false)
        secondItem = PhotoPickerItem(2, uriWrapper, false)
    }

    @Test
    fun `loads data on refresh`() = test {
        setupViewModel(listOf(firstItem), singleSelectBrowserType)

        viewModel.refreshData(singleSelectBrowserType, false)

        assertThat(uiModels).hasSize(1)
        assertUiModel(singleSelectBrowserType, selectedItems = listOf(), domainItems = listOf(firstItem))
    }

    @Test
    fun `selects single item with single selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)

        viewModel.refreshData(singleSelectBrowserType, false)

        assertThat(uiModels).hasSize(1)
        assertUiModel(
                singleSelectBrowserType,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(0)

        assertThat(uiModels).hasSize(2)
        assertUiModel(
                singleSelectBrowserType,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `clears selection`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)

        viewModel.refreshData(singleSelectBrowserType, false)

        selectItem(0)
        viewModel.clearSelection()

        assertThat(uiModels).hasSize(3)

        assertUiModel(
                singleSelectBrowserType,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `unselects first item when second item selected with single selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), singleSelectBrowserType)

        viewModel.refreshData(singleSelectBrowserType, false)

        assertUiModel(
                singleSelectBrowserType,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(0)

        assertUiModel(
                singleSelectBrowserType,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(1)

        assertUiModel(
                singleSelectBrowserType,
                selectedItems = listOf(secondItem),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `selects two items with multi selection available`() = test {
        setupViewModel(listOf(firstItem, secondItem), multiSelectBrowserType)

        viewModel.refreshData(multiSelectBrowserType, false)

        assertUiModel(
                multiSelectBrowserType,
                selectedItems = listOf(),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(1)

        assertUiModel(
                multiSelectBrowserType,
                selectedItems = listOf(secondItem),
                domainItems = listOf(firstItem, secondItem)
        )
        selectItem(0)

        assertUiModel(
                multiSelectBrowserType,
                selectedItems = listOf(secondItem, firstItem),
                domainItems = listOf(firstItem, secondItem)
        )

        selectItem(1)

        assertUiModel(
                multiSelectBrowserType,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
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

        viewModel.refreshData(singleSelectBrowserType, false)

        assertThat(navigateEvents).isEmpty()
        clickItem(0)
        assertThat(navigateEvents).isNotEmpty
        verify(analyticsTrackerWrapper).track(eq(MEDIA_PICKER_PREVIEW_OPENED), any<MutableMap<String, Any>>())
    }

    private fun selectItem(position: Int) {
        uiModels.last().items[position].toggleAction.toggle()
    }

    private fun clickItem(position: Int) {
        uiModels.last().items[position].clickAction.click()
    }

    private fun assertUiModel(
        browserType: MediaBrowserType,
        selectedItems: List<PhotoPickerItem>,
        domainItems: List<PhotoPickerItem>
    ) {
        uiModels.last().apply {
            assertThat(this.browserType).isEqualTo(browserType)
            assertThat(this.count).isEqualTo(selectedItems.size)
            assertThat(this.isVideoSelected).isFalse()
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
        assertThat(viewModel.numSelected()).isEqualTo(selectedItems.size)
        assertThat(viewModel.selectedURIs()).containsAll(selectedItems.map { it.uri })
    }

    private suspend fun setupViewModel(
        domainModel: List<PhotoPickerItem>,
        browserType: MediaBrowserType
    ) {
        whenever(deviceMediaListBuilder.buildDeviceMedia(browserType)).thenReturn(domainModel)
        viewModel.data.observeForever {
            if (it != null) {
                uiModels.add(it)
            }
        }
        viewModel.navigateToPreview.observeForever {
            if (it != null) {
                navigateEvents.add(it)
            }
        }
        viewModel.start(listOf(), browserType)
        assertThat(uiModels).isEmpty()
    }

    private fun PhotoPickerUiModel.assertSelection(
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

    private fun PhotoPickerUiItem.assertEqualToDomainItem(domainItem: PhotoPickerItem) {
        assertThat(this.id).isEqualTo(domainItem.id)
        assertThat(this.isVideo).isEqualTo(domainItem.isVideo)
        assertThat(this.uri).isEqualTo(domainItem.uri)
    }
}
