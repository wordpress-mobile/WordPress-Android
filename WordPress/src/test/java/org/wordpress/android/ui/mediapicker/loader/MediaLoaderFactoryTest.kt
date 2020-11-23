package org.wordpress.android.ui.mediapicker.loader

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.CameraSetup.HIDDEN
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.ui.mediapicker.loader.DeviceListBuilder.DeviceListBuilderFactory
import org.wordpress.android.ui.mediapicker.loader.MediaLibraryDataSource.MediaLibraryDataSourceFactory
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class MediaLoaderFactoryTest {
    @Mock lateinit var deviceListBuilderFactory: DeviceListBuilderFactory
    @Mock lateinit var deviceListBuilder: DeviceListBuilder
    @Mock lateinit var mediaLibraryDataSourceFactory: MediaLibraryDataSourceFactory
    @Mock lateinit var mediaLibraryDataSource: MediaLibraryDataSource
    @Mock lateinit var stockMediaDataSource: StockMediaDataSource
    @Mock lateinit var gifMediaDataSource: GifMediaDataSource
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var site: SiteModel
    private lateinit var mediaLoaderFactory: MediaLoaderFactory

    @Before
    fun setUp() {
        mediaLoaderFactory = MediaLoaderFactory(
                deviceListBuilderFactory,
                mediaLibraryDataSourceFactory,
                stockMediaDataSource,
                gifMediaDataSource,
                localeManagerWrapper,
                networkUtilsWrapper
        )
    }

    @Test
    fun `returns device list builder on DEVICE source`() {
        val mediaPickerSetup = MediaPickerSetup(
                DEVICE,
                availableDataSources = setOf(),
                canMultiselect = true,
                requiresStoragePermissions = true,
                allowedTypes = setOf(),
                cameraSetup = HIDDEN,
                systemPickerEnabled = true,
                editingEnabled = true,
                queueResults = false,
                defaultSearchView = false,
                title = string.wp_media_title
        )
        whenever(deviceListBuilderFactory.build(setOf())).thenReturn(deviceListBuilder)
        val mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)

        assertThat(mediaLoader).isEqualTo(
                MediaLoader(
                        deviceListBuilder,
                        localeManagerWrapper,
                        networkUtilsWrapper
                )
        )
    }

    @Test
    fun `returns WP media source on WP_LIBRARY source`() {
        val mediaPickerSetup = MediaPickerSetup(
                WP_LIBRARY,
                availableDataSources = setOf(),
                canMultiselect = true,
                requiresStoragePermissions = false,
                allowedTypes = setOf(),
                cameraSetup = HIDDEN,
                systemPickerEnabled = false,
                editingEnabled = false,
                queueResults = false,
                defaultSearchView = false,
                title = string.wp_media_title
        )
        whenever(mediaLibraryDataSourceFactory.build(site, setOf())).thenReturn(mediaLibraryDataSource)

        val mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)

        assertThat(mediaLoader).isEqualTo(
                MediaLoader(
                        mediaLibraryDataSource,
                        localeManagerWrapper,
                        networkUtilsWrapper
                )
        )
    }

    @Test
    fun `returns stock media source on STOCK_LIBRARY source`() {
        val mediaPickerSetup = MediaPickerSetup(
                STOCK_LIBRARY,
                availableDataSources = setOf(),
                canMultiselect = true,
                requiresStoragePermissions = false,
                allowedTypes = setOf(),
                cameraSetup = HIDDEN,
                systemPickerEnabled = false,
                editingEnabled = false,
                queueResults = false,
                defaultSearchView = false,
                title = string.wp_media_title
        )

        val mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)

        assertThat(mediaLoader).isEqualTo(
                MediaLoader(
                        stockMediaDataSource,
                        localeManagerWrapper,
                        networkUtilsWrapper
                )
        )
    }

    @Test
    fun `returns gif media source on GIF_LIBRARY source`() {
        val mediaPickerSetup = MediaPickerSetup(
                GIF_LIBRARY,
                availableDataSources = setOf(),
                canMultiselect = true,
                requiresStoragePermissions = false,
                allowedTypes = setOf(),
                cameraSetup = HIDDEN,
                systemPickerEnabled = false,
                editingEnabled = false,
                queueResults = false,
                defaultSearchView = true,
                title = string.photo_picker_gif
        )

        val mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)

        assertThat(mediaLoader).isEqualTo(
                MediaLoader(
                        gifMediaDataSource,
                        localeManagerWrapper,
                        networkUtilsWrapper
                )
        )
    }
}
