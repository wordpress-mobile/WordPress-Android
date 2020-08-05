package org.wordpress.android.ui.photopicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.ClickAction
import org.wordpress.android.ui.photopicker.PhotoPickerUiItem.ToggleAction
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PhotoPickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val deviceMediaListBuilder: DeviceMediaListBuilder,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _data = MutableLiveData<List<PhotoPickerItem>>()
    private val _selectedIds = MutableLiveData<List<Long>>()
    private val _browserType = MutableLiveData<MediaBrowserType>()
    private val _navigateToPreview = MutableLiveData<Event<UriWrapper>>()
    val navigateToPreview: LiveData<Event<UriWrapper>> = _navigateToPreview
    val selectedIds: LiveData<List<Long>> = _selectedIds
    val data: LiveData<PhotoPickerUiModel> = merge(
            _data,
            _selectedIds,
            _browserType
    ) { data, selectedIds, browserType ->
        var isVideoSelected = false
        if (data != null && browserType != null) {
            PhotoPickerUiModel(data.map {
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
            }, selectedIds?.size ?: 0, isVideoSelected, browserType)
        } else {
            null
        }
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

    data class PhotoPickerUiModel(
        val items: List<PhotoPickerUiItem>,
        val count: Int = 0,
        val isVideoSelected: Boolean = false,
        val browserType: MediaBrowserType
    )
}
