package org.wordpress.android.ui.mediapicker.loader

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.ui.mediapicker.loader.DeviceListBuilder.DeviceListBuilderFactory
import org.wordpress.android.ui.mediapicker.loader.MediaLibraryDataSource.MediaLibraryDataSourceFactory
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class MediaLoaderFactory
@Inject constructor(
    private val deviceListBuilderFactory: DeviceListBuilderFactory,
    private val mediaLibraryDataSourceFactory: MediaLibraryDataSourceFactory,
    private val stockMediaDataSource: StockMediaDataSource,
    private val gifMediaDataSource: GifMediaDataSource,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    fun build(mediaPickerSetup: MediaPickerSetup, siteModel: SiteModel?): MediaLoader {
        return when (mediaPickerSetup.primaryDataSource) {
            DEVICE -> deviceListBuilderFactory.build(mediaPickerSetup.allowedTypes)
            WP_LIBRARY -> mediaLibraryDataSourceFactory.build(requireNotNull(siteModel) {
                "Site is necessary when loading WP media library "
            }, mediaPickerSetup.allowedTypes)
            STOCK_LIBRARY -> stockMediaDataSource
            GIF_LIBRARY -> gifMediaDataSource
        }.toMediaLoader()
    }

    private fun MediaSource.toMediaLoader() = MediaLoader(this, localeManagerWrapper, networkUtilsWrapper)
}
