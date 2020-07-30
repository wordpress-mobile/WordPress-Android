package org.wordpress.android.ui.photopicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PhotoPickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
        private val deviceMediaListBuilder: DeviceMediaListBuilder
) : ScopedViewModel(mainDispatcher) {
    private val _data = MutableLiveData<List<PhotoPickerItem>>()
    val data: LiveData<List<PhotoPickerItem>> = _data
    fun refreshData(browserType: MediaBrowserType, forceReload: Boolean) {
        launch(bgDispatcher) {
            val result = deviceMediaListBuilder.buildDeviceMedia(browserType)
            val currentItems = _data.value ?: listOf()
            if (forceReload || currentItems != result) {
                _data.postValue(result)
            }
        }
    }
}
