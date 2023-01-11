package org.wordpress.android.ui.mediapicker.insert

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.ui.mediapicker.insert.DeviceListInsertUseCase.DeviceListInsertUseCaseFactory
import org.wordpress.android.ui.mediapicker.insert.GifMediaInsertUseCase.GifMediaInsertUseCaseFactory
import org.wordpress.android.ui.mediapicker.insert.StockMediaInsertUseCase.StockMediaInsertUseCaseFactory
import javax.inject.Inject

class MediaInsertHandlerFactory
@Inject constructor(
    private val deviceListInsertUseCaseFactory: DeviceListInsertUseCaseFactory,
    private val stockMediaInsertUseCaseFactory: StockMediaInsertUseCaseFactory,
    private val gifMediaInsertUseCaseFactory: GifMediaInsertUseCaseFactory
) {
    fun build(mediaPickerSetup: MediaPickerSetup, siteModel: SiteModel?): MediaInsertHandler {
        return when (mediaPickerSetup.primaryDataSource) {
            DEVICE -> deviceListInsertUseCaseFactory.build(mediaPickerSetup.queueResults)
            WP_LIBRARY -> DefaultMediaInsertUseCase
            STOCK_LIBRARY -> stockMediaInsertUseCaseFactory.build(requireNotNull(siteModel) {
                "Site is necessary when inserting into stock media library "
            })
            GIF_LIBRARY -> gifMediaInsertUseCaseFactory.build(requireNotNull(siteModel) {
                "Site is necessary when inserting into gif media library "
            })
        }.toMediaInsertHandler()
    }

    private fun MediaInsertUseCase.toMediaInsertHandler() = MediaInsertHandler(this)

    private object DefaultMediaInsertUseCase : MediaInsertUseCase
}
