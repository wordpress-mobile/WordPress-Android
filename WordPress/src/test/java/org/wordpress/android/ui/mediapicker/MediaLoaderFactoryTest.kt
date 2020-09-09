package org.wordpress.android.ui.mediapicker

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mediapicker.MediaLibraryDataSource.MediaLibraryDataSourceFactory
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY
import org.wordpress.android.util.LocaleManagerWrapper

@RunWith(MockitoJUnitRunner::class)
class MediaLoaderFactoryTest {
    @Mock lateinit var deviceListBuilder: DeviceListBuilder
    @Mock lateinit var mediaLibraryDataSourceFactory: MediaLibraryDataSourceFactory
    @Mock lateinit var mediaLibraryDataSource: MediaLibraryDataSource
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var site: SiteModel
    private lateinit var mediaLoaderFactory: MediaLoaderFactory

    @Before
    fun setUp() {
        mediaLoaderFactory = MediaLoaderFactory(deviceListBuilder, mediaLibraryDataSourceFactory, localeManagerWrapper)
    }

    @Test
    fun `returns device list builder on DEVICE source`() {
        val mediaPickerSetup = MediaPickerSetup(DEVICE, true, setOf(), false, R.string.wp_media_title)
        val mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)

        assertThat(mediaLoader).isEqualTo(
                MediaLoader(
                        deviceListBuilder,
                        localeManagerWrapper,
                        mediaPickerSetup.allowedTypes
                )
        )
    }

    @Test
    fun `returns WP media source on WP_LIBRARY source`() {
        val mediaPickerSetup = MediaPickerSetup(WP_LIBRARY, true, setOf(), false, R.string.wp_media_title)
        whenever(mediaLibraryDataSourceFactory.build(site)).thenReturn(mediaLibraryDataSource)

        val mediaLoader = mediaLoaderFactory.build(mediaPickerSetup, site)

        assertThat(mediaLoader).isEqualTo(
                MediaLoader(
                        mediaLibraryDataSource,
                        localeManagerWrapper,
                        mediaPickerSetup.allowedTypes
                )
        )
    }

    @Test
    fun `throws exception on not implemented sources`() {
        assertThatExceptionOfType(NotImplementedError::class.java).isThrownBy {
            mediaLoaderFactory.build(
                    MediaPickerSetup(GIF_LIBRARY, true, setOf(), false, R.string.wp_media_title),
                    site
            )
        }
        assertThatExceptionOfType(NotImplementedError::class.java).isThrownBy {
            mediaLoaderFactory.build(
                    MediaPickerSetup(STOCK_LIBRARY, true, setOf(), false, R.string.wp_media_title),
                    site
            )
        }
    }
}
