package org.wordpress.android.ui.photopicker

import android.Manifest.permission
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.AZTEC_EDITOR_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.GRAVATAR_IMAGE_PICKER
import org.wordpress.android.ui.media.MediaBrowserType.SITE_ICON_PICKER
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel.ProgressDialogUiModel
import org.wordpress.android.ui.posts.editor.media.CopyMediaToAppStorageUseCase
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.ViewWrapper
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.distinct
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@Deprecated(
        "This class is being refactored, if you implement any change, please also update " +
                "{@link org.wordpress.android.ui.mediapicker.MediaPickerViewModel}"
)
class PhotoPickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Suppress("DEPRECATION") private val deviceMediaListBuilder: DeviceMediaListBuilder,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val permissionsHandler: PermissionsHandler,
    private val resourceProvider: ResourceProvider,
    private val copyMediaToAppStorageUseCase: CopyMediaToAppStorageUseCase,
    private val getMediaModelUseCase: GetMediaModelUseCase
) : ScopedViewModel(mainDispatcher) {
    private val _navigateToPreview = MutableLiveData<Event<UriWrapper>>()
    private val _onInsert = MutableLiveData<Event<List<UriWrapper>>>()
    private val _showPopupMenu = MutableLiveData<Event<PopupMenuUiModel>>()
    @Suppress("DEPRECATION") private val _photoPickerItems = MutableLiveData<List<PhotoPickerItem>>()
    private val _selectedIds = MutableLiveData<List<Long>?>()
    private val _onIconClicked = MutableLiveData<Event<IconClickEvent>>()
    private val _onPermissionsRequested = MutableLiveData<Event<PermissionsRequested>>()
    private val _softAskRequest = MutableLiveData<SoftAskRequest>()
    private val _showProgressDialog = MutableLiveData<ProgressDialogUiModel>()

    val onNavigateToPreview: LiveData<Event<UriWrapper>> = _navigateToPreview
    val onInsert: LiveData<Event<List<UriWrapper>>> = _onInsert
    val onIconClicked: LiveData<Event<IconClickEvent>> = _onIconClicked

    val onShowPopupMenu: LiveData<Event<PopupMenuUiModel>> = _showPopupMenu
    val onPermissionsRequested: LiveData<Event<PermissionsRequested>> = _onPermissionsRequested

    val selectedIds: LiveData<List<Long>?> = _selectedIds

    @Suppress("DEPRECATION") val uiState: LiveData<PhotoPickerUiState> = merge(
            _photoPickerItems.distinct(),
            _selectedIds.distinct(),
            _softAskRequest,
            _showProgressDialog
    ) { photoPickerItems, selectedIds, softAskRequest, progressDialogModel ->
        PhotoPickerUiState(
                buildPhotoPickerUiModel(photoPickerItems, selectedIds),
                buildBottomBar(
                        photoPickerItems,
                        selectedIds,
                        softAskRequest?.show == true
                ),
                buildSoftAskView(softAskRequest),
                FabUiModel(browserType.isWPStoriesPicker && selectedIds.isNullOrEmpty()) {
                    clickIcon(PhotoPickerFragment.PhotoPickerIcon.WP_STORIES_CAPTURE)
                },
                buildActionModeUiModel(selectedIds),
                progressDialogModel ?: ProgressDialogUiModel.Hidden
        )
    }

    @Suppress("DEPRECATION") var lastTappedIcon: PhotoPickerFragment.PhotoPickerIcon? = null
    private lateinit var browserType: MediaBrowserType
    private var site: SiteModel? = null

    @Suppress("DEPRECATION")
    private fun buildPhotoPickerUiModel(
        data: List<PhotoPickerItem>?,
        selectedIds: List<Long>?
    ): PhotoListUiModel {
        var isVideoSelected = false
        return if (data != null) {
            val uiItems = data.map {
                val showOrderCounter = browserType.canMultiselect()
                val toggleAction = PhotoPickerUiItem.ToggleAction(it.id, showOrderCounter, this::toggleItem)
                val clickAction = PhotoPickerUiItem.ClickAction(it.uri, it.isVideo, this::clickItem)
                val (selectedOrder, isSelected) = if (selectedIds != null && selectedIds.contains(it.id)) {
                    isVideoSelected = isVideoSelected || it.isVideo
                    val selectedOrder = if (showOrderCounter) selectedIds.indexOf(it.id) + 1 else null
                    val isSelected = true
                    selectedOrder to isSelected
                } else {
                    null to false
                }
                if (it.isVideo) {
                    PhotoPickerUiItem.VideoItem(
                            id = it.id,
                            uri = it.uri,
                            isSelected = isSelected,
                            selectedOrder = selectedOrder,
                            showOrderCounter = showOrderCounter,
                            toggleAction = toggleAction,
                            clickAction = clickAction
                    )
                } else {
                    PhotoPickerUiItem.PhotoItem(
                            id = it.id,
                            uri = it.uri,
                            isSelected = isSelected,
                            selectedOrder = selectedOrder,
                            showOrderCounter = showOrderCounter,
                            toggleAction = toggleAction,
                            clickAction = clickAction
                    )
                }
            }
            PhotoListUiModel.Data(uiItems)
        } else {
            PhotoListUiModel.Empty
        }
    }

    private fun buildActionModeUiModel(
        selectedIds: List<Long>?
    ): ActionModeUiModel {
        val numSelected = selectedIds?.size ?: 0
        if (numSelected == 0) {
            return ActionModeUiModel.Hidden
        }
        val title: UiString? = when {
            numSelected == 0 -> null
            browserType.canMultiselect() -> {
                UiStringText(String.format(resourceProvider.getString(R.string.cab_selected), numSelected))
            }
            else -> {
                if (browserType.isImagePicker && browserType.isVideoPicker) {
                    UiStringRes(R.string.photo_picker_use_media)
                } else if (browserType.isVideoPicker) {
                    UiStringRes(R.string.photo_picker_use_video)
                } else {
                    UiStringRes(R.string.photo_picker_use_photo)
                }
            }
        }
        return ActionModeUiModel.Visible(
                title,
                showConfirmAction = !browserType.isGutenbergPicker
        )
    }

    @Suppress("DEPRECATION")
    private fun buildBottomBar(
        photoPickerItems: List<PhotoPickerItem>?,
        selectedIds: List<Long>?,
        showSoftAskViewModel: Boolean
    ): BottomBarUiModel {
        val count = selectedIds?.size ?: 0
        val isVideoSelected = photoPickerItems?.any { it.isVideo && selectedIds?.contains(it.id) == true } ?: false
        val defaultBottomBar = when {
            showSoftAskViewModel -> BottomBarUiModel.BottomBar.NONE
            count <= 0 -> BottomBarUiModel.BottomBar.MEDIA_SOURCE
            browserType.isGutenbergPicker -> BottomBarUiModel.BottomBar.INSERT_EDIT
            else -> BottomBarUiModel.BottomBar.NONE
        }

        val insertEditTextBarVisible = count != 0 && browserType.isGutenbergPicker && !isVideoSelected
        val showCamera = !browserType.isGutenbergPicker && !browserType.isWPStoriesPicker
        return BottomBarUiModel(
                type = defaultBottomBar,
                insertEditTextBarVisible = insertEditTextBarVisible,
                hideMediaBottomBarInPortrait = browserType == AZTEC_EDITOR_PICKER,
                showCameraButton = showCamera,
                showWPMediaIcon = site != null && !browserType.isGutenbergPicker,
                canShowInsertEditBottomBar = browserType.isGutenbergPicker,
                onIconPickerClicked = { v ->
                    if (browserType == GRAVATAR_IMAGE_PICKER || browserType == SITE_ICON_PICKER) {
                        clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO)
                    } else {
                        performActionOrShowPopup(v)
                    }
                }
        )
    }

    fun refreshData(forceReload: Boolean) {
        if (!permissionsHandler.hasWriteStoragePermission()) {
            return
        }
        launch(bgDispatcher) {
            val result = deviceMediaListBuilder.buildDeviceMedia(browserType)
            val currentItems = _photoPickerItems.value ?: listOf()
            if (forceReload || currentItems != result) {
                _photoPickerItems.postValue(result)
            }
        }
    }

    fun clearSelection() {
        if (!_selectedIds.value.isNullOrEmpty()) {
            _selectedIds.postValue(listOf())
        }
    }

    @Suppress("DEPRECATION")
    fun start(
        selectedIds: List<Long>?,
        browserType: MediaBrowserType,
        lastTappedIcon: PhotoPickerFragment.PhotoPickerIcon?,
        site: SiteModel?
    ) {
        _selectedIds.value = selectedIds
        this.browserType = browserType
        this.lastTappedIcon = lastTappedIcon
        this.site = site
    }

    fun numSelected(): Int {
        return _selectedIds.value?.size ?: 0
    }

    fun selectedURIs(): List<UriWrapper> {
        val items = (uiState.value?.photoListUiModel as? PhotoListUiModel.Data)?.items
        return _selectedIds.value?.mapNotNull { id -> items?.find { it.id == id }?.uri } ?: listOf()
    }

    private fun toggleItem(id: Long, canMultiselect: Boolean) {
        val updatedIds = _selectedIds.value?.toMutableList() ?: mutableListOf()
        if (updatedIds.contains(id)) {
            updatedIds.remove(id)
        } else {
            if (updatedIds.isNotEmpty() && !canMultiselect) {
                updatedIds.clear()
            }
            updatedIds.add(id)
        }
        _selectedIds.postValue(updatedIds)
    }

    private fun clickItem(uri: UriWrapper?, isVideo: Boolean) {
        trackOpenPreviewScreenEvent(uri, isVideo)
        uri?.let {
            _navigateToPreview.postValue(Event(it))
        }
    }

    private fun trackOpenPreviewScreenEvent(uri: UriWrapper?, isVideo: Boolean) {
        launch(bgDispatcher) {
            val properties = analyticsUtilsWrapper.getMediaProperties(
                    isVideo,
                    uri,
                    null
            )
            properties["is_video"] = isVideo
            analyticsTrackerWrapper.track(MEDIA_PICKER_PREVIEW_OPENED, properties)
        }
    }

    fun performInsertAction() {
        val uriList = selectedURIs()
        _onInsert.value = Event(uriList)
        val isMultiselection = uriList.size > 1
        for (mediaUri in uriList) {
            val isVideo = MediaUtils.isVideo(mediaUri.toString())
            val properties = analyticsUtilsWrapper.getMediaProperties(
                    isVideo,
                    mediaUri,
                    null
            )
            properties["is_part_of_multiselection"] = isMultiselection
            if (isMultiselection) {
                properties["number_of_media_selected"] = uriList.size
            }
            analyticsTrackerWrapper.track(MEDIA_PICKER_RECENT_MEDIA_SELECTED, properties)
        }
    }

    fun clickOnLastTappedIcon() = clickIcon(lastTappedIcon!!)

    @Suppress("DEPRECATION")
    fun clickIcon(icon: PhotoPickerFragment.PhotoPickerIcon) {
        if (icon == PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_PHOTO ||
                icon == PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_VIDEO ||
                icon == PhotoPickerFragment.PhotoPickerIcon.WP_STORIES_CAPTURE
        ) {
            if (!permissionsHandler.hasPermissionsToAccessPhotos()) {
                _onPermissionsRequested.value = Event(PermissionsRequested.CAMERA)
                lastTappedIcon = icon
                return
            }
        }
        when (icon) {
            PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_PHOTO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
                    false
            )
            PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_VIDEO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
                    true
            )
            PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
                    false
            )
            PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_VIDEO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
                    true
            )
            PhotoPickerFragment.PhotoPickerIcon.WP_MEDIA -> AnalyticsTracker.track(MEDIA_PICKER_OPEN_WP_MEDIA)
            PhotoPickerFragment.PhotoPickerIcon.STOCK_MEDIA -> Unit // Do nothing
            PhotoPickerFragment.PhotoPickerIcon.GIF -> Unit // Do nothing
            PhotoPickerFragment.PhotoPickerIcon.WP_STORIES_CAPTURE -> AnalyticsTracker.track(
                    MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE
            )
            PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO_OR_VIDEO -> Unit // Do nothing
        }
        _onIconClicked.postValue(Event(IconClickEvent(icon, browserType.canMultiselect())))
    }

    private fun trackSelectedOtherSourceEvents(stat: Stat, isVideo: Boolean) {
        val properties: MutableMap<String, Any?> = HashMap()
        properties["is_video"] = isVideo
        AnalyticsTracker.track(stat, properties)
    }

    @Suppress("DEPRECATION")
    fun onCameraClicked(viewWrapper: ViewWrapper) {
        if (browserType.isImagePicker && browserType.isVideoPicker) {
            showCameraPopupMenu(viewWrapper)
        } else if (browserType.isImagePicker) {
            clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_PHOTO)
        } else if (browserType.isVideoPicker) {
            clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_VIDEO)
        } else {
            AppLog.e(
                    MEDIA,
                    "This code should be unreachable. If you see this message one of " +
                            "the MediaBrowserTypes isn't setup correctly."
            )
        }
    }

    @Suppress("DEPRECATION")
    fun showCameraPopupMenu(viewWrapper: ViewWrapper) {
        val capturePhotoItem = PopupMenuUiModel.PopupMenuItem(UiStringRes(R.string.photo_picker_capture_photo)) {
            clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_PHOTO)
        }
        val captureVideoItem = PopupMenuUiModel.PopupMenuItem(UiStringRes(R.string.photo_picker_capture_video)) {
            clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_VIDEO)
        }
        _showPopupMenu.value = Event(PopupMenuUiModel(viewWrapper, listOf(capturePhotoItem, captureVideoItem)))
    }

    @Suppress("DEPRECATION")
    fun performActionOrShowPopup(viewWrapper: ViewWrapper) {
        val items = mutableListOf<PopupMenuUiModel.PopupMenuItem>()
        if (browserType.isImagePicker) {
            items.add(PopupMenuUiModel.PopupMenuItem(UiStringRes(R.string.photo_picker_choose_photo)) {
                clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO)
            })
        }
        if (browserType.isVideoPicker) {
            items.add(PopupMenuUiModel.PopupMenuItem(UiStringRes(R.string.photo_picker_choose_video)) {
                clickIcon(PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_VIDEO)
            })
        }
        if (site != null && !browserType.isGutenbergPicker) {
            items.add(PopupMenuUiModel.PopupMenuItem(UiStringRes(R.string.photo_picker_stock_media)) {
                clickIcon(PhotoPickerFragment.PhotoPickerIcon.STOCK_MEDIA)
            })
            // only show GIF picker from Tenor if this is NOT the WPStories picker
            if (!browserType.isWPStoriesPicker) {
                items.add(PopupMenuUiModel.PopupMenuItem(UiStringRes(R.string.photo_picker_gif)) {
                    clickIcon(PhotoPickerFragment.PhotoPickerIcon.GIF)
                })
            }
        }
        if (items.size == 1) {
            items[0].action()
        } else {
            _showPopupMenu.value = Event(PopupMenuUiModel(viewWrapper, items))
        }
    }

    fun checkStoragePermission(isAlwaysDenied: Boolean) {
        if (permissionsHandler.hasWriteStoragePermission()) {
            _softAskRequest.value = SoftAskRequest(show = false, isAlwaysDenied = isAlwaysDenied)
            if (_photoPickerItems.value.isNullOrEmpty()) {
                refreshData(false)
            }
        } else {
            _softAskRequest.value = SoftAskRequest(show = true, isAlwaysDenied = isAlwaysDenied)
        }
    }

    private fun buildSoftAskView(softAskRequest: SoftAskRequest?): SoftAskViewUiModel {
        if (softAskRequest != null && softAskRequest.show) {
            val appName = "<strong>${resourceProvider.getString(R.string.app_name)}</strong>"
            val label = if (softAskRequest.isAlwaysDenied) {
                val permissionName = ("<strong>${
                    WPPermissionUtils.getPermissionName(
                            resourceProvider,
                            permission.WRITE_EXTERNAL_STORAGE
                    )
                }</strong>")
                String.format(
                        resourceProvider.getString(R.string.photo_picker_soft_ask_permissions_denied), appName,
                        permissionName
                )
            } else {
                String.format(
                        resourceProvider.getString(R.string.photo_picker_soft_ask_label),
                        appName
                )
            }
            val allowId = if (softAskRequest.isAlwaysDenied) {
                R.string.button_edit_permissions
            } else {
                R.string.photo_picker_soft_ask_allow
            }
            return SoftAskViewUiModel.Visible(label, UiStringRes(allowId), softAskRequest.isAlwaysDenied)
        } else {
            return SoftAskViewUiModel.Hidden
        }
    }

    fun urisSelectedFromSystemPicker(uris: List<UriWrapper>) {
        copySelectedUrisLocally(uris)
    }

    fun mediaIdsSelectedFromWPMediaPicker(mediaIds: List<Long>) {
        launch {
            val mediaModels = getMediaModelUseCase
                    .loadMediaByRemoteId(requireNotNull(site), mediaIds)
            copySelectedUrisLocally(mediaModels.map { UriWrapper(Uri.parse(it.url)) })
        }
    }

    fun copySelectedUrisLocally(uris: List<UriWrapper>) {
        launch {
            _showProgressDialog.value = ProgressDialogUiModel.Visible(R.string.uploading_title) {
                _showProgressDialog.postValue(ProgressDialogUiModel.Hidden)
                cancel()
            }
            val localUris = copyMediaToAppStorageUseCase.copyFilesToAppStorageIfNecessary(uris.map { it.uri })
            _showProgressDialog.postValue(ProgressDialogUiModel.Hidden)
            if (isActive) {
                _onInsert.value = Event(localUris.permanentlyAccessibleUris.map { UriWrapper(it) })
            }
        }
    }

    data class PhotoPickerUiState(
        val photoListUiModel: PhotoListUiModel,
        val bottomBarUiModel: BottomBarUiModel,
        val softAskViewUiModel: SoftAskViewUiModel,
        val fabUiModel: FabUiModel,
        val actionModeUiModel: ActionModeUiModel,
        val progressDialogUiModel: ProgressDialogUiModel
    )

    @Suppress("DEPRECATION")
    sealed class PhotoListUiModel {
        data class Data(val items: List<PhotoPickerUiItem>) :
                PhotoListUiModel()

        object Empty : PhotoListUiModel()
    }

    data class BottomBarUiModel(
        val type: BottomBar,
        val insertEditTextBarVisible: Boolean,
        val hideMediaBottomBarInPortrait: Boolean,
        val showCameraButton: Boolean,
        val showWPMediaIcon: Boolean,
        val canShowInsertEditBottomBar: Boolean,
        val onIconPickerClicked: (ViewWrapper) -> Unit
    ) {
        enum class BottomBar {
            INSERT_EDIT, MEDIA_SOURCE, NONE
        }
    }

    sealed class SoftAskViewUiModel {
        data class Visible(val label: String, val allowId: UiStringRes, val isAlwaysDenied: Boolean) :
                SoftAskViewUiModel()

        object Hidden : SoftAskViewUiModel()
    }

    data class FabUiModel(val show: Boolean, val action: () -> Unit)

    sealed class ActionModeUiModel {
        data class Visible(
            val actionModeTitle: UiString? = null,
            val showConfirmAction: Boolean = false
        ) : ActionModeUiModel()

        object Hidden : ActionModeUiModel()
    }

    @Suppress("DEPRECATION")
    data class IconClickEvent(
        val icon: PhotoPickerFragment.PhotoPickerIcon,
        val allowMultipleSelection: Boolean
        )

    enum class PermissionsRequested {
        CAMERA, STORAGE
    }

    data class PopupMenuUiModel(val view: ViewWrapper, val items: List<PopupMenuItem>) {
        data class PopupMenuItem(val title: UiStringRes, val action: () -> Unit)
    }

    data class SoftAskRequest(val show: Boolean, val isAlwaysDenied: Boolean)
}
