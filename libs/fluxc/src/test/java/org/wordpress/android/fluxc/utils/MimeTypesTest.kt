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
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/mspowerpoint",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                        "application/vnd.oasis.opendocument.text",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/keynote",
                        "application/zip"
                )
        )
    }
}
