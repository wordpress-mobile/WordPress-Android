package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MimeTypesTest {
    private val mimeTypes = MimeTypes()

    @Test
    fun `returns all mime types as strings`() {
        val allTypes = mimeTypes.getAllTypes()

        assertThat(allTypes).isEqualTo(
                arrayOf(
                        "audio/mpeg",
                        "audio/mp4",
                        "audio/ogg",
                        "application/ogg",
                        "audio/x-wav",
                        "video/mp4",
                        "video/quicktime",
                        "video/x-ms-wmv",
                        "video/avi",
                        "video/mpeg",
                        "video/mp2p",
                        "video/ogg",
                        "video/3gpp",
                        "video/3gpp2",
                        "image/jpeg",
                        "image/png",
                        "image/gif",
                        "application/pdf",
                        "application/msword",
                        "application/doc",
                        "application/ms-doc",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/powerpoint",
                        "application/mspowerpoint",
                        "application/x-mspowerpoint",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                        "application/vnd.oasis.opendocument.text",
                        "application/excel",
                        "application/x-excel",
                        "application/vnd.ms-excel",
                        "application/x-msexcel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/keynote",
                        "application/zip"
                )
        )
    }

    @Test
    fun `returns image and video only mime types as strings`() {
        val allTypes = mimeTypes.getVideoAndImageTypesOnly()

        assertThat(allTypes).isEqualTo(
                arrayOf(
                        "video/mp4",
                        "video/quicktime",
                        "video/x-ms-wmv",
                        "video/avi",
                        "video/mpeg",
                        "video/mp2p",
                        "video/ogg",
                        "video/3gpp",
                        "video/3gpp2",
                        "image/jpeg",
                        "image/png",
                        "image/gif"
                )
        )
    }

    @Test
    fun `returns image only mime types as strings`() {
        val allTypes = mimeTypes.getImageTypesOnly()

        assertThat(allTypes).isEqualTo(
                arrayOf(
                        "image/jpeg",
                        "image/png",
                        "image/gif"
                )
        )
    }

    @Test
    fun `returns video only mime types as strings`() {
        val allTypes = mimeTypes.getVideoTypesOnly()

        assertThat(allTypes).isEqualTo(
                arrayOf(
                        "video/mp4",
                        "video/quicktime",
                        "video/x-ms-wmv",
                        "video/avi",
                        "video/mpeg",
                        "video/mp2p",
                        "video/ogg",
                        "video/3gpp",
                        "video/3gpp2"
                )
        )
    }

    @Test
    fun `returns audio only mime types as strings`() {
        val allTypes = mimeTypes.getAudioTypesOnly()

        assertThat(allTypes).isEqualTo(
                arrayOf(
                        "audio/mpeg",
                        "audio/mp4",
                        "audio/ogg",
                        "application/ogg",
                        "audio/x-wav"
                )
        )
    }
}
