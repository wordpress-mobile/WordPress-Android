package org.wordpress.android.ui.mediapicker

import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

class MediaLoaderFactory
@Inject constructor(
    private val deviceListBuilder: DeviceListBuilder,
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    fun build(mediaSourceType: MediaPickerSetup.DataSource): MediaLoader {
        return when (mediaSourceType) {
            DEVICE -> MediaLoader(deviceListBuilder, localeManagerWrapper)
            WP_LIBRARY -> throw NotImplementedError("Source not implemented yet")
            STOCK_LIBRARY -> throw NotImplementedError("Source not implemented yet")
            GIF_LIBRARY -> throw NotImplementedError("Source not implemented yet")
        }
    }
}
