package org.wordpress.android.ui.photopicker.mediapicker

import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import javax.inject.Inject

class MediaLoaderFactory
@Inject constructor(private val deviceListBuilder: DeviceListBuilder) {
    fun build(mediaSourceType: MediaPickerSetup.DataSource): MediaLoader {
        return when (mediaSourceType) {
            DEVICE -> MediaLoader(deviceListBuilder)
            WP_LIBRARY -> TODO()
            STOCK_LIBRARY -> TODO()
            GIF_LIBRARY -> TODO()
        }
    }
}
