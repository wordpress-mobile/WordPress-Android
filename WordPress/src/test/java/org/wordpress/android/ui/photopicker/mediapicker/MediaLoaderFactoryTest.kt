package org.wordpress.android.ui.photopicker.mediapicker

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.GIF_LIBRARY
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.STOCK_LIBRARY
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerSetup.DataSource.WP_LIBRARY

@RunWith(MockitoJUnitRunner::class)
class MediaLoaderFactoryTest {
    @Mock lateinit var deviceListBuilder: DeviceListBuilder
    private lateinit var mediaLoaderFactory: MediaLoaderFactory

    @Before
    fun setUp() {
        mediaLoaderFactory = MediaLoaderFactory(deviceListBuilder)
    }

    @Test
    fun `returns device list builder on DEVICE source`() {
        val mediaLoader = mediaLoaderFactory.build(DEVICE)

        assertThat(mediaLoader).isEqualTo(MediaLoader(deviceListBuilder))
    }

    @Test
    fun `throws exception on not implemented sources`() {
        assertThatExceptionOfType(NotImplementedError::class.java).isThrownBy { mediaLoaderFactory.build(GIF_LIBRARY) }
        assertThatExceptionOfType(NotImplementedError::class.java).isThrownBy { mediaLoaderFactory.build(STOCK_LIBRARY) }
        assertThatExceptionOfType(NotImplementedError::class.java).isThrownBy { mediaLoaderFactory.build(WP_LIBRARY) }
    }
}
