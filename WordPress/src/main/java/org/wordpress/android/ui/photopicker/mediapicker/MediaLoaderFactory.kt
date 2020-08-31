package org.wordpress.android.ui.photopicker.mediapicker

import javax.inject.Inject

class MediaLoaderFactory
@Inject constructor(private val deviceListBuilder: DeviceListBuilder) {
    fun build(): MediaLoader {
        return MediaLoader(deviceListBuilder)
    }
}
