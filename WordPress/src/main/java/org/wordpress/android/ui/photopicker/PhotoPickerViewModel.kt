package org.wordpress.android.ui.photopicker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_CAPTURE_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_MEDIA
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaBrowserType.AZTEC_EDITOR_PICKER
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_PHOTO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CAPTURE_VIDEO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_PHOTO_OR_VIDEO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.ANDROID_CHOOSE_VIDEO
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.GIF
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.STOCK_MEDIA
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.WP_MEDIA
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon.WP_STORIES_CAPTURE
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.ClickAction
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.ToggleAction
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel.BottomBar
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel.BottomBar.INSERT_EDIT
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel.PhotoPickerUiModel.BottomBarUiModel.BottomBar.MEDIA_SOURCE
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named

class PhotoPickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val deviceMediaListBuilder: DeviceMediaListBuilder,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val context: Context
) : ScopedViewModel(mainDispatcher) {
    private val _navigateToPreview = MutableLiveData<Event<UriWrapper>>()
    private val _showActionMode = MutableLiveData<Event<Boolean>>()
    private val _onInsert = MutableLiveData<Event<List<UriWrapper>>>()
    private val _browserType = MutableLiveData<MediaBrowserType>()
    private val _data = MutableLiveData<List<PhotoPickerItem>>()
    private val _selectedIds = MutableLiveData<List<Long>>()
    private val _bottomBar = MutableLiveData<BottomBar>()
    private val _onIconClicked = MutableLiveData<PhotoPickerIcon>()

    val navigateToPreview: LiveData<Event<UriWrapper>> = _navigateToPreview
    val onInsert: LiveData<Event<List<UriWrapper>>> = _onInsert
    val onIconClicked: LiveData<Event<IconClickEvent>> = merge(_onIconClicked, _browserType) { icon, browserType ->
        if (icon != null && browserType != null) {
            Event(IconClickEvent(icon, browserType.canMultiselect()))
        } else {
            null
        }
    }

    val showActionMode: LiveData<Event<Boolean>> = _showActionMode
    private val _actionModeUiModel = MutableLiveData<ActionModeUiModel>()
    val actionModeUiModel: LiveData<ActionModeUiModel> = merge(_browserType, _selectedIds) { browserType, selectedIds ->
        if (browserType == null) {
            return@merge null
        }
        val numSelected = selectedIds?.size ?: 0
        val title: UiString? = when {
            numSelected == 0 -> null
            browserType.canMultiselect() -> {
                UiStringText(String.format(context.resources.getString(string.cab_selected), numSelected))
            }
            else -> {
                if (browserType.isImagePicker && browserType.isVideoPicker) {
                    UiStringRes(string.photo_picker_use_media)
                } else if (browserType.isVideoPicker) {
                    UiStringRes(string.photo_picker_use_video)
                } else {
                    UiStringRes(string.photo_picker_use_photo)
                }
            }
        }
        ActionModeUiModel(title, browserType.isGutenbergPicker)
    }

    val selectedIds: LiveData<List<Long>> = _selectedIds
    val data: LiveData<PhotoPickerUiModel> = merge(
            _data,
            _selectedIds,
            _browserType,
            _bottomBar
    ) { data, selectedIds, browserType, bottomBar ->
        var isVideoSelected = false
        if (data != null && browserType != null) {
            val uiItems = data.map {
                if (selectedIds != null && selectedIds.contains(it.id)) {
                    isVideoSelected = isVideoSelected || it.isVideo
                    PhotoPickerUiItem(
                            id = it.id,
                            uri = it.uri,
                            isVideo = it.isVideo,
                            isSelected = true,
                            selectedOrder = if (browserType.canMultiselect()) selectedIds.indexOf(it.id) + 1 else null,
                            showOrderCounter = browserType.canMultiselect(),
                            toggleAction = ToggleAction(it.id, browserType.canMultiselect(), this::toggleItem),
                            clickAction = ClickAction(it.id, it.uri, it.isVideo, this::clickItem)
                    )
                } else {
                    PhotoPickerUiItem(
                            id = it.id,
                            uri = it.uri,
                            isVideo = it.isVideo,
                            isSelected = false,
                            selectedOrder = null,
                            showOrderCounter = browserType.canMultiselect(),
                            toggleAction = ToggleAction(it.id, browserType.canMultiselect(), this::toggleItem),
                            clickAction = ClickAction(it.id, it.uri, it.isVideo, this::clickItem)
                    )
                }
            }
            val count = selectedIds?.size ?: 0
            if (count == 0 && _showActionMode.value?.peekContent() == true) {
                _showActionMode.postValue(Event(false))
            } else if (count > 0 && _showActionMode.value?.peekContent() != true) {
                _showActionMode.postValue(Event(true))
            }
            PhotoPickerUiModel(
                    uiItems,
                    count,
                    isVideoSelected,
                    browserType,
                    buildBottomBar(bottomBar, browserType, count, isVideoSelected)
            )
        } else {
            null
        }
    }

    private fun buildBottomBar(
        bottomBar: BottomBar?,
        browserType: MediaBrowserType,
        count: Int,
        isVideoSelected: Boolean
    ): BottomBarUiModel {
        val insertEditTextBarVisible = count != 0 && browserType.isGutenbergPicker && isVideoSelected
        val showCamera = !browserType.isGutenbergPicker && !browserType.isWPStoriesPicker
        return BottomBarUiModel(
                type = bottomBar ?: MEDIA_SOURCE,
                insertEditTextBarVisible = insertEditTextBarVisible,
                hideMediaBottomBarInPortrait = browserType == AZTEC_EDITOR_PICKER,
                showCameraButton = showCamera
        )
    }

    fun refreshData(browserType: MediaBrowserType, forceReload: Boolean) {
        launch(bgDispatcher) {
            val result = deviceMediaListBuilder.buildDeviceMedia(browserType)
            val currentItems = _data.value ?: listOf()
            if (forceReload || currentItems != result) {
                _data.postValue(result)
            }
        }
    }

    fun clearSelection() {
        if (!_selectedIds.value.isNullOrEmpty()) {
            _selectedIds.postValue(listOf())
        }
    }

    fun start(selectedIds: List<Long>?, browserType: MediaBrowserType) {
        selectedIds?.let {
            _selectedIds.value = selectedIds
        }
        _browserType.value = browserType
    }

    fun numSelected(): Int {
        return _selectedIds.value?.size ?: 0
    }

    fun selectedURIs(): List<UriWrapper> {
        return data.value?.items?.mapNotNull { if (it.isSelected) it.uri else null } ?: listOf()
    }

    fun showInsertEditBottomBar() {
        _bottomBar.value = INSERT_EDIT
    }

    fun showMediaSourceBottomBar() {
        _bottomBar.value = MEDIA_SOURCE
    }

    private fun toggleItem(id: Long, canMultiselect: Boolean) {
        val updatedIds = if (canMultiselect) _selectedIds.value?.toMutableList() ?: mutableListOf() else mutableListOf()
        if (updatedIds.contains(id)) {
            updatedIds.remove(id)
        } else {
            updatedIds.add(id)
        }
        _selectedIds.postValue(updatedIds)
    }

    private fun clickItem(id: Long, uri: UriWrapper?, isVideo: Boolean) {
        trackOpenPreviewScreenEvent(id, uri, isVideo)
        uri?.let {
            _navigateToPreview.postValue(Event(it))
        }
    }

    private fun trackOpenPreviewScreenEvent(id: Long, uri: UriWrapper?, isVideo: Boolean) {
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

    fun finishActionMode() {
        if (showActionMode.value?.peekContent() == true) {
            _showActionMode.postValue(Event(false))
        }
    }

    fun clickIcon(icon: PhotoPickerIcon) {
        when (icon) {
            ANDROID_CAPTURE_PHOTO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
                    false
            )
            ANDROID_CAPTURE_VIDEO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_CAPTURE_MEDIA,
                    true
            )
            ANDROID_CHOOSE_PHOTO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
                    false
            )
            ANDROID_CHOOSE_VIDEO -> trackSelectedOtherSourceEvents(
                    MEDIA_PICKER_OPEN_DEVICE_LIBRARY,
                    true
            )
            WP_MEDIA -> AnalyticsTracker.track(MEDIA_PICKER_OPEN_WP_MEDIA)
            STOCK_MEDIA -> {
            }
            GIF -> {
            }
            WP_STORIES_CAPTURE -> AnalyticsTracker.track(MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE)
            ANDROID_CHOOSE_PHOTO_OR_VIDEO -> {
            }
        }
        _onIconClicked.postValue(icon)
    }

    private fun trackSelectedOtherSourceEvents(stat: Stat, isVideo: Boolean) {
        val properties: MutableMap<String, Any?> = HashMap()
        properties["is_video"] = isVideo
        AnalyticsTracker.track(stat, properties)
    }

    data class PhotoPickerUiModel(
        val items: List<PhotoPickerUiItem>,
        val count: Int = 0,
        val isVideoSelected: Boolean = false,
        val browserType: MediaBrowserType,
        val bottomBarUiModel: BottomBarUiModel
    ) {
        data class BottomBarUiModel(
            val type: BottomBar,
            val insertEditTextBarVisible: Boolean,
            val hideMediaBottomBarInPortrait: Boolean,
            val showCameraButton: Boolean
        ) {
            enum class BottomBar {
                INSERT_EDIT, MEDIA_SOURCE, NONE
            }
        }
    }

    data class ActionModeUiModel(val actionModeTitle: UiString? = null, val showInsertEditBottomBar: Boolean = false)

    data class IconClickEvent(val icon: PhotoPickerIcon, val allowMultipleSelection: Boolean)
}
