package org.wordpress.android.ui.mediapicker

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.utils.MimeTypes
import org.wordpress.android.test
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.LocalUri
import org.wordpress.android.ui.mediapicker.MediaNavigationEvent.IconClickEvent
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.ChooserContext
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.OpenSystemPicker
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerAction.SwitchMediaPicker
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.ChooseFromAndroidDevice
import org.wordpress.android.ui.mediapicker.MediaPickerFragment.MediaPickerIcon.SwitchSource
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.ENABLED
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.HIDDEN
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.STORIES
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.FileItem
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.NextPageLoader
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.PhotoItem
import org.wordpress.android.ui.mediapicker.MediaPickerUiItem.VideoItem
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ActionModeUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.SYSTEM_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.BrowseMenuUiModel.BrowseAction.WP_MEDIA_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.EditActionUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.MediaPickerUiState
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.PhotoListUiModel.Data
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.PhotoListUiModel.Empty
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.PhotoListUiModel.Hidden
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.SearchUiModel
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.SoftAskViewUiModel
import org.wordpress.android.ui.mediapicker.MediaType.AUDIO
import org.wordpress.android.ui.mediapicker.MediaType.DOCUMENT
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.MediaType.VIDEO
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandler
import org.wordpress.android.ui.mediapicker.insert.MediaInsertHandlerFactory
import org.wordpress.android.ui.mediapicker.loader.MediaLoader
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.DomainModel
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction
import org.wordpress.android.ui.mediapicker.loader.MediaLoaderFactory
import org.wordpress.android.ui.photopicker.PermissionsHandler
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale

@Suppress("LargeClass")
class MediaPickerViewModelTest : BaseUnitTest() {
    @Mock lateinit var mediaLoaderFactory: MediaLoaderFactory
    @Mock lateinit var mediaLoader: MediaLoader
    @Mock lateinit var mediaInsertHandlerFactory: MediaInsertHandlerFactory
    @Mock lateinit var mediaInsertHandler: MediaInsertHandler
    @Mock lateinit var mediaPickerTracker: MediaPickerTracker
    @Mock lateinit var uriWrapper1: UriWrapper
    @Mock lateinit var uriWrapper2: UriWrapper
    @Mock lateinit var permissionsHandler: PermissionsHandler
    @Mock lateinit var context: Context
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var mediaStore: MediaStore
    private lateinit var viewModel: MediaPickerViewModel
    private var uiStates = mutableListOf<MediaPickerUiState>()
    private lateinit var actions: Channel<LoadAction>
    private var navigateEvents = mutableListOf<Event<MediaNavigationEvent>>()
    private val singleSelectMediaPickerSetup = buildMediaPickerSetup(false, setOf(IMAGE))
    private val multiSelectMediaPickerSetup = buildMediaPickerSetup(true, setOf(IMAGE, VIDEO))
    private val singleSelectVideoPickerSetup = buildMediaPickerSetup(false, setOf(VIDEO))
    private val singleSelectAudioPickerSetup = buildMediaPickerSetup(false, setOf(AUDIO))
    private val multiSelectFilePickerSetup = buildMediaPickerSetup(true, setOf(IMAGE, VIDEO, AUDIO, DOCUMENT))
    private val site = SiteModel()
    private lateinit var firstItem: MediaItem
    private lateinit var secondItem: MediaItem
    private lateinit var videoItem: MediaItem
    private lateinit var audioItem: MediaItem
    private lateinit var documentItem: MediaItem

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = MediaPickerViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                mediaLoaderFactory,
                mediaInsertHandlerFactory,
                mediaPickerTracker,
                permissionsHandler,
                localeManagerWrapper,
                mediaUtilsWrapper,
                mediaStore,
                resourceProvider
        )
        uiStates.clear()
        val identifier1 = LocalUri(uriWrapper1)
        val identifier2 = LocalUri(uriWrapper2)
        firstItem = MediaItem(identifier1, "url://item1", "item1", IMAGE, "image/jpg", 1)
        secondItem = MediaItem(identifier2, "url://item2", "item2", IMAGE, "image/png", 2)
        videoItem = MediaItem(identifier1, "url://item3", "item3", VIDEO, "video/mpeg", 3)
        audioItem = MediaItem(identifier2, "url://item4", "item4", AUDIO, "audio/mp3", 4)
        documentItem = MediaItem(identifier2, "url://item5", "item5", DOCUMENT, "application/pdf", 5)
        whenever(mediaUtilsWrapper.getExtensionForMimeType("image/jpg")).thenReturn("jpg")
        whenever(mediaUtilsWrapper.getExtensionForMimeType("image/png")).thenReturn("png")
        whenever(mediaUtilsWrapper.getExtensionForMimeType("audio/mp3")).thenReturn("mp3")
        whenever(mediaUtilsWrapper.getExtensionForMimeType("video/mpeg")).thenReturn("mpg")
        whenever(mediaUtilsWrapper.getExtensionForMimeType("application/pdf")).thenReturn("pdf")
        whenever(mediaUtilsWrapper.getSitePlanForMimeTypes(site)).thenReturn(MimeTypes.Plan.NO_PLAN_SPECIFIED)
        whenever(localeManagerWrapper.getLocale()).thenReturn(Locale.US)
    }

    @Test
    fun `loads data on refresh`() = test {
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertDataList(singleSelectMediaPickerSetup, selectedItems = listOf(), domainItems = listOf(firstItem))
        assertActionModeHidden()
        verify(mediaPickerTracker).trackMediaPickerOpened(singleSelectMediaPickerSetup)
    }

    @Test
    fun `adds loading item when source has more data`() = test {
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup, hasMore = true)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(),
                domainItems = listOf(firstItem),
                showsLoadingItem = true
        )
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

        verify(mediaPickerTracker).trackItemSelected(singleSelectMediaPickerSetup)
        assertThat(uiStates).hasSize(3)
        assertDataList(
                singleSelectMediaPickerSetup,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
        assertActionModeVisible(
                UiStringRes(R.string.photo_picker_use_photo),
                EditActionUiModel(isVisible = true, isCounterBadgeVisible = false)
        )
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
        verify(mediaPickerTracker).trackSelectionCleared(singleSelectMediaPickerSetup)
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
        inOrder(mediaPickerTracker) {
            verify(mediaPickerTracker, times(2)).trackItemSelected(multiSelectMediaPickerSetup)
            verify(mediaPickerTracker).trackItemUnselected(multiSelectMediaPickerSetup)
        }
        assertDataList(
                multiSelectMediaPickerSetup,
                selectedItems = listOf(firstItem),
                domainItems = listOf(firstItem, secondItem)
        )
    }

    @Test
    fun `navigates to preview on item click`() = test {
        whenever(mediaUtilsWrapper.getRealPathFromURI(anyOrNull())).thenReturn(firstItem.url)
        setupViewModel(listOf(firstItem, secondItem), singleSelectMediaPickerSetup)

        viewModel.refreshData(false)

        assertThat(navigateEvents).isEmpty()
        clickItem()
        assertThat(navigateEvents).isNotEmpty
        verify(mediaPickerTracker).trackPreview(
                firstItem.type == VIDEO,
                firstItem.identifier,
                singleSelectMediaPickerSetup
        )
    }

    @Test
    fun `shows soft ask screen when storage permissions are turned off`() = test {
        setupViewModel(listOf(), singleSelectMediaPickerSetup, hasStoragePermissions = false)
        whenever(resourceProvider.getString(R.string.app_name)).thenReturn("WordPress")
        whenever(resourceProvider.getString(R.string.photo_picker_soft_ask_label)).thenReturn("Soft ask label")
        val isAlwaysDenied = false

        viewModel.checkStoragePermission(isAlwaysDenied = isAlwaysDenied)

        assertThat(uiStates).hasSize(3)

        assertSoftAskUiModelVisible(isAlwaysDenied, singleSelectMediaPickerSetup)
    }

    @Test
    fun `action mode title is Use Photo when photo browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), buildMediaPickerSetup(false, setOf(IMAGE)))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(
                UiStringRes(R.string.photo_picker_use_photo),
                EditActionUiModel(isVisible = true, isCounterBadgeVisible = false)
        )
    }

    @Test
    fun `action mode title is Use Video when video browser type`() = test {
        setupViewModel(
                listOf(firstItem, secondItem),
                buildMediaPickerSetup(false, setOf(VIDEO), editingEnabled = false)
        )

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringRes(R.string.photo_picker_use_video), EditActionUiModel(isVisible = false))
    }

    @Test
    fun `action mode title is Use Media when image and video browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), buildMediaPickerSetup(false, setOf(IMAGE, VIDEO)))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(
                UiStringRes(R.string.photo_picker_use_media),
                EditActionUiModel(isVisible = true, isCounterBadgeVisible = false)
        )
    }

    @Test
    fun `action mode title is Use Audio when audio browser type`() = test {
        setupViewModel(listOf(firstItem, secondItem), buildMediaPickerSetup(false, setOf(AUDIO)))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(
                UiStringRes(R.string.photo_picker_use_audio),
                EditActionUiModel(isVisible = true, isCounterBadgeVisible = false)
        )
    }

    @Test
    fun `action mode title is Select N items when multi selection available`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem, secondItem), buildMediaPickerSetup(true, setOf(IMAGE)))

        viewModel.refreshData(false)

        selectItem(0)
        assertActionModeVisible(
                UiStringText("1 selected"),
                EditActionUiModel(isVisible = true, isCounterBadgeVisible = true, counterBadgeValue = 1)
        )

        selectItem(1)
        assertActionModeVisible(
                UiStringText("2 selected"),
                EditActionUiModel(isVisible = true, isCounterBadgeVisible = true, counterBadgeValue = 2)
        )
    }

    @Test
    fun `action mode hides edit action when video item selected`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(videoItem, secondItem), buildMediaPickerSetup(true, setOf(IMAGE, VIDEO)))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringText("1 selected"), EditActionUiModel(isVisible = false))
    }

    @Test
    fun `action mode hides edit action when audio item selected`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(audioItem, secondItem), buildMediaPickerSetup(true, setOf(IMAGE, AUDIO)))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringText("1 selected"), EditActionUiModel(isVisible = false))
    }

    @Test
    fun `action mode hides edit action when document item selected`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(documentItem, secondItem), buildMediaPickerSetup(true, setOf(IMAGE, DOCUMENT)))

        viewModel.refreshData(false)

        selectItem(0)

        assertActionModeVisible(UiStringText("1 selected"), EditActionUiModel(isVisible = false))
    }

    @Test
    fun `on search expanded updates state`() = test {
        val query = "filter"
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup, filter = query)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertSearchCollapsed()

        viewModel.onSearchExpanded()

        verify(mediaPickerTracker).trackSearchExpanded(singleSelectMediaPickerSetup)
        assertSearchExpanded(query)
    }

    @Test
    fun `on search searches for results`() = test {
        val query = "dog"
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup, filter = query)

        assertThat(uiStates).hasSize(2)

        viewModel.onSearch(query)

        verify(mediaPickerTracker).trackSearch(singleSelectMediaPickerSetup)
        assertThat(uiStates).hasSize(2)
    }

    @Test
    fun `system picker opened for photo when allowed types is IMAGE only`() = test {
        setupViewModel(listOf(), singleSelectMediaPickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(SYSTEM_PICKER)

        verify(mediaPickerTracker).trackIconClick(
                ChooseFromAndroidDevice(singleSelectMediaPickerSetup.allowedTypes),
                singleSelectMediaPickerSetup
        )
        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is OpenSystemPicker).isTrue()
        assertThat((iconClickEvents[0].action as OpenSystemPicker).chooserContext).isEqualTo(ChooserContext.PHOTO)
    }

    @Test
    fun `system picker opened for video when allowed types is VIDEO only`() = test {
        setupViewModel(listOf(), singleSelectVideoPickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(SYSTEM_PICKER)

        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is OpenSystemPicker).isTrue()
        assertThat((iconClickEvents[0].action as OpenSystemPicker).chooserContext).isEqualTo(ChooserContext.VIDEO)
    }

    @Test
    fun `system picker opened for image and video when allowed types is IMAGE and VIDEO`() = test {
        setupViewModel(listOf(), multiSelectMediaPickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(SYSTEM_PICKER)

        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is OpenSystemPicker).isTrue()
        assertThat((iconClickEvents[0].action as OpenSystemPicker).chooserContext)
                .isEqualTo(ChooserContext.PHOTO_OR_VIDEO)
    }

    @Test
    fun `system picker opened for audio when allowed types is AUDIO only`() = test {
        setupViewModel(listOf(), singleSelectAudioPickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(SYSTEM_PICKER)

        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is OpenSystemPicker).isTrue()
        assertThat((iconClickEvents[0].action as OpenSystemPicker).chooserContext).isEqualTo(ChooserContext.AUDIO)
    }

    @Test
    fun `system picker opened for all supported files when is browser picker`() = test {
        setupViewModel(listOf(), multiSelectFilePickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(SYSTEM_PICKER)

        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is OpenSystemPicker).isTrue()
        assertThat((iconClickEvents[0].action as OpenSystemPicker).chooserContext).isEqualTo(ChooserContext.MEDIA_FILE)
    }

    @Test
    fun `switch media source from DEVICE to WP_MEDIA_LIBRARY`() = test {
        val mediaPickerSetup = singleSelectMediaPickerSetup.copy(
                availableDataSources = setOf(WP_LIBRARY),
                cameraSetup = ENABLED
        )
        setupViewModel(listOf(), mediaPickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(WP_MEDIA_LIBRARY)

        verify(mediaPickerTracker).trackIconClick(
                SwitchSource(WP_LIBRARY),
                mediaPickerSetup
        )
        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is SwitchMediaPicker).isTrue()
        val updatedMediaPickerSetup = (iconClickEvents[0].action as SwitchMediaPicker).mediaPickerSetup
        assertThat(updatedMediaPickerSetup).isEqualTo(
                mediaPickerSetup.copy(
                        primaryDataSource = WP_LIBRARY,
                        availableDataSources = setOf(),
                        systemPickerEnabled = false,
                        cameraSetup = HIDDEN
                )
        )
    }

    @Test
    fun `switch media source from DEVICE to STOCK_LIBRARY`() = test {
        val mediaPickerSetup = singleSelectMediaPickerSetup.copy(availableDataSources = setOf(STOCK_LIBRARY))
        setupViewModel(listOf(), mediaPickerSetup, true)

        val iconClickEvents = mutableListOf<IconClickEvent>()

        viewModel.onNavigate.observeForever {
            it.peekContent().let { clickEvent ->
                if (clickEvent is IconClickEvent) {
                    iconClickEvents.add(clickEvent)
                }
            }
        }

        viewModel.onMenuItemClicked(BrowseAction.STOCK_LIBRARY)

        verify(mediaPickerTracker).trackIconClick(
                SwitchSource(STOCK_LIBRARY),
                mediaPickerSetup
        )
        assertThat(iconClickEvents).hasSize(1)
        assertThat(iconClickEvents[0].action is SwitchMediaPicker).isTrue()
        val updatedMediaPickerSetup = (iconClickEvents[0].action as SwitchMediaPicker).mediaPickerSetup
        assertThat(updatedMediaPickerSetup).isEqualTo(
                mediaPickerSetup.copy(
                        primaryDataSource = STOCK_LIBRARY,
                        availableDataSources = setOf(),
                        defaultSearchView = true,
                        systemPickerEnabled = false
                )
        )
    }

    @Test
    fun `camera FAB is shown in stories when no selected items`() = test {
        setupViewModel(listOf(firstItem), buildMediaPickerSetup(true, setOf(IMAGE, VIDEO), STORIES))
        assertStoriesFabIsVisible()
    }

    @Test
    fun `camera FAB is not shown in stories when selected items`() = test {
        whenever(resourceProvider.getString(R.string.cab_selected)).thenReturn("%d selected")
        setupViewModel(listOf(firstItem), buildMediaPickerSetup(true, setOf(IMAGE, VIDEO), STORIES))

        selectItem(0)

        assertStoriesFabIsHidden()
    }

    @Test
    fun `camera FAB is not shown when no stories`() = test {
        setupViewModel(listOf(firstItem), buildMediaPickerSetup(true, setOf(IMAGE, VIDEO), HIDDEN))

        assertStoriesFabIsHidden()
    }

    @Test
    fun `empty state is emitted when no items in picker`() = test {
        setupViewModel(null, singleSelectMediaPickerSetup, numberOfStates = 1)

        viewModel.checkStoragePermission(isAlwaysDenied = false)

        assertThat(uiStates).hasSize(2)
        assertPhotoListUiStateEmpty()
    }

    @Test
    fun `hidden state is emitted when when need to ask permission in picker`() = test {
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup, hasStoragePermissions = false)
        whenever(resourceProvider.getString(R.string.app_name)).thenReturn("WordPress")
        whenever(resourceProvider.getString(R.string.photo_picker_soft_ask_label)).thenReturn("Soft ask label")

        viewModel.checkStoragePermission(isAlwaysDenied = false)

        assertThat(uiStates).hasSize(3)
        assertPhotoListUiStateHidden()
    }

    @Test
    fun `data items state is emitted when items available in picker and have permissions`() = test {
        setupViewModel(listOf(firstItem), singleSelectMediaPickerSetup, hasStoragePermissions = true)

        viewModel.refreshData(false)

        assertThat(uiStates).hasSize(2)
        assertPhotoListUiStateData()
    }

    @Test
    fun `does not start loading without storage permissions`() = test {
        setupViewModel(
                listOf(firstItem),
                singleSelectMediaPickerSetup.copy(requiresStoragePermissions = true),
                hasStoragePermissions = false
        )

        assertThat(actions.tryReceive().getOrNull()).isNull()
    }

    @Test
    fun `starts loading with storage permissions`() = test {
        setupViewModel(
                listOf(firstItem),
                singleSelectMediaPickerSetup.copy(requiresStoragePermissions = true),
                hasStoragePermissions = true
        )

        assertThat(actions.tryReceive().getOrNull()).isEqualTo(LoadAction.Start(null))
    }

    @Test
    fun `starts loading when storage permissions not necessary`() = test {
        setupViewModel(
                listOf(firstItem),
                singleSelectMediaPickerSetup.copy(requiresStoragePermissions = false),
                hasStoragePermissions = false
        )

        assertThat(actions.tryReceive().getOrNull()).isEqualTo(LoadAction.Start(null))
    }

    private fun selectItem(position: Int) {
        when (val item = itemOnPosition(position)) {
            is PhotoItem -> item.toggleAction.toggle()
            is VideoItem -> item.toggleAction.toggle()
            is FileItem -> item.toggleAction.toggle()
            is NextPageLoader -> Unit // Do nothing
        }
    }

    private fun clickItem() {
        when (val item = itemOnPosition(0)) {
            is PhotoItem -> item.clickAction.click()
            is VideoItem -> item.clickAction.click()
            is FileItem -> item.clickAction.click()
            is NextPageLoader -> item.loadAction()
        }
    }

    private fun itemOnPosition(position: Int) =
            (uiStates.last().photoListUiModel as Data).items[position]

    private fun assertDataList(
        mediaPickerSetup: MediaPickerSetup,
        selectedItems: List<MediaItem>,
        domainItems: List<MediaItem>,
        showsLoadingItem: Boolean = false
    ) {
        uiStates.last().apply {
            assertThat(this.photoListUiModel).isNotNull()
            (uiStates.last().photoListUiModel as Data).apply {
                if (showsLoadingItem) {
                    assertThat(this.items).hasSize(domainItems.size + 1)
                    assertThat(this.items.last() is NextPageLoader).isTrue()
                } else {
                    assertThat(this.items).hasSize(domainItems.size)
                }
                domainItems.forEachIndexed { index, photoPickerItem ->
                    val isSelected = selectedItems.any { it.identifier == photoPickerItem.identifier }
                    assertSelection(
                            position = index,
                            isSelected = isSelected,
                            domainItem = photoPickerItem,
                            selectedOrder = selectedItems.indexOfFirst { it.identifier == photoPickerItem.identifier },
                            isMultiSelection = mediaPickerSetup.canMultiselect
                    )
                }
            }
        }
        assertThat(viewModel.numSelected()).isEqualTo(selectedItems.size)
        assertThat(viewModel.selectedIdentifiers()).isEqualTo(selectedItems.map { it.identifier })
        assertSoftAskUiModelHidden()
    }

    private fun assertSoftAskUiModelVisible(isAlwaysDenied: Boolean, mediaPickerSetup: MediaPickerSetup) {
        uiStates.last().softAskViewUiModel.let {
            val model = it as SoftAskViewUiModel.Visible
            assertThat(model.allowId).isEqualTo(UiStringRes(R.string.photo_picker_soft_ask_allow))
            assertThat(model.isAlwaysDenied).isEqualTo(isAlwaysDenied)
            assertThat(model.label).isEqualTo("Soft ask label")
        }
        verify(mediaPickerTracker).trackShowPermissionsScreen(mediaPickerSetup, isAlwaysDenied)
    }

    private fun assertSoftAskUiModelHidden() {
        uiStates.last().softAskViewUiModel.let {
            assertThat(it is SoftAskViewUiModel.Hidden).isTrue()
        }
    }

    private fun assertPhotoListUiStateData() {
        uiStates.last().photoListUiModel.let {
            assertThat(it is Data).isTrue()
        }
    }

    private fun assertPhotoListUiStateEmpty() {
        uiStates.last().photoListUiModel.let {
            assertThat(it is Empty).isTrue()
        }
    }

    private fun assertPhotoListUiStateHidden() {
        uiStates.last().photoListUiModel.let {
            assertThat(it is Hidden).isTrue()
        }
    }

    private suspend fun setupViewModel(
        domainModel: List<MediaItem>?,
        mediaPickerSetup: MediaPickerSetup,
        hasStoragePermissions: Boolean = true,
        filter: String? = null,
        numberOfStates: Int = 2,
        hasMore: Boolean = false
    ) {
        whenever(permissionsHandler.hasStoragePermission()).thenReturn(hasStoragePermissions)
        whenever(mediaLoaderFactory.build(mediaPickerSetup, site)).thenReturn(mediaLoader)
        doAnswer {
            actions = it.getArgument(0)
            return@doAnswer flow {
                if (null != domainModel) {
                    emit(
                            DomainModel(
                                    domainModel,
                                    filter = filter,
                                    hasMore = hasMore
                            )
                    )
                }
            }
        }.whenever(mediaLoader).loadMedia(any())

        whenever(mediaInsertHandlerFactory.build(mediaPickerSetup, site)).thenReturn(mediaInsertHandler)

        viewModel.start(listOf(), mediaPickerSetup, null, site)
        viewModel.uiState.observeForever {
            if (it != null) {
                uiStates.add(it)
            }
        }
        viewModel.onNavigate.observeForever {
            if (it != null) {
                navigateEvents.add(it)
            }
        }
        assertThat(uiStates).hasSize(numberOfStates)
    }

    private fun Data.assertSelection(
        position: Int,
        isSelected: Boolean,
        isMultiSelection: Boolean = false,
        selectedOrder: Int,
        domainItem: MediaItem
    ) {
        val mediaPickerItem = this.items[position]
        mediaPickerItem.toSelectableItem()!!.apply {
            assertThat(this.isSelected).isEqualTo(isSelected)
            if (isSelected && isMultiSelection) {
                assertThat(this.selectedOrder).isEqualTo(selectedOrder + 1)
            } else {
                assertThat(this.selectedOrder).isNull()
            }
            assertThat(this.showOrderCounter).isEqualTo(isMultiSelection)
        }
        mediaPickerItem.assertEqualToDomainItem(domainItem)
    }

    private fun MediaPickerUiItem.assertEqualToDomainItem(domainItem: MediaItem) {
        when (domainItem.type) {
            IMAGE -> assertThat((this as PhotoItem).identifier).isEqualTo(domainItem.identifier)
            VIDEO -> assertThat((this as VideoItem).identifier).isEqualTo(domainItem.identifier)
            DOCUMENT, AUDIO -> assertThat((this as FileItem).identifier).isEqualTo(domainItem.identifier)
        }
    }

    private fun assertActionModeHidden() {
        uiStates.last().actionModeUiModel.let { model ->
            assertThat(model is ActionModeUiModel.Hidden).isTrue()
        }
    }

    private fun assertActionModeVisible(title: UiString, editActionUiModel: EditActionUiModel) {
        uiStates.last().actionModeUiModel.let {
            val model = it as ActionModeUiModel.Visible
            assertThat(model.actionModeTitle).isEqualTo(title)
            assertThat(model.editActionUiModel).isEqualTo(editActionUiModel)
        }
    }

    private fun assertSearchCollapsed() {
        uiStates.last().searchUiModel.let { model ->
            assertThat(model is SearchUiModel.Collapsed).isTrue()
        }
    }

    private fun assertSearchExpanded(filter: String) {
        uiStates.last().searchUiModel.let { model ->
            assertThat(model is SearchUiModel.Expanded).isTrue()
            assertThat((model as SearchUiModel.Expanded).filter).isEqualTo(filter)
        }
    }

    private fun buildMediaPickerSetup(
        canMultiselect: Boolean,
        allowedTypes: Set<MediaType>,
        cameraSetup: CameraSetup = HIDDEN,
        editingEnabled: Boolean = true,
        requiresStoragePermissions: Boolean = true
    ) = MediaPickerSetup(
            primaryDataSource = DEVICE,
            availableDataSources = setOf(),
            canMultiselect = canMultiselect,
            requiresStoragePermissions = requiresStoragePermissions,
            allowedTypes = allowedTypes,
            cameraSetup = cameraSetup,
            systemPickerEnabled = true,
            editingEnabled = editingEnabled,
            queueResults = false,
            defaultSearchView = false,
            title = R.string.wp_media_title
    )

    private fun assertStoriesFabIsVisible() {
        uiStates.last().fabUiModel.let { model ->
            assertThat(model.show).isEqualTo(true)
        }
    }

    private fun assertStoriesFabIsHidden() {
        uiStates.last().fabUiModel.let { model ->
            assertThat(model.show).isEqualTo(false)
        }
    }
}
