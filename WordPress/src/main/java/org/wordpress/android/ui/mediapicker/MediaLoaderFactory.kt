package org.wordpress.android.ui.mediapicker

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaLibraryDataSource.MediaLibraryDataSourceFactory
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.util.LocaleManagerWrapper
import javax.inject.Inject

class MediaLoaderFactory
@Inject constructor(
    private val deviceListBuilder: DeviceListBuilder,
    private val mediaLibraryDataSourceFactory: MediaLibraryDataSourceFactory,
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    fun build(mediaSourceType: MediaPickerSetup.DataSource, siteModel: SiteModel?): MediaLoader {
        return when (mediaSourceType) {
            DEVICE -> deviceListBuilder
            WP_LIBRARY -> mediaLibraryDataSourceFactory.build(requireNotNull(siteModel) {
                "Site is necessary when loading WP media library "
            })
            STOCK_LIBRARY -> throw NotImplementedError("Source not implemented yet")
            GIF_LIBRARY -> throw NotImplementedError("Source not implemented yet")
        }.toMediaLoader()
    }

    private fun MediaSource.toMediaLoader() = MediaLoader(this, localeManagerWrapper)
}
