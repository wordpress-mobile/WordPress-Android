package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MediaFileNameUtilsTest {
    /* BUILD PROCESSED FILE NAME */

    @Test
    fun `given a temp file name, when building the processed name, then the original name is restored`() {
        val result = MediaFileNameUtils.buildProcessedFileName("PXL_20240314_120000.jpg", "PXL_202403141864.jpg")

        assertThat(result).isEqualTo("PXL_20240314_120000.jpg")
    }

    @Test
    fun `given a processed file with another extension, when building the processed name, then it is kept`() {
        val result = MediaFileNameUtils.buildProcessedFileName("holidays.heic", "holidays1864.jpg")

        assertThat(result).isEqualTo("holidays.jpg")
    }

    @Test
    fun `given a processed file without extension, when building the processed name, then the original one is used`() {
        val result = MediaFileNameUtils.buildProcessedFileName("my holiday photo.jpg", "my holiday photo1864.")

        assertThat(result).isEqualTo("my holiday photo.jpg")
    }

    @Test
    fun `given no extension at all, when building the processed name, then the name has no extension`() {
        val result = MediaFileNameUtils.buildProcessedFileName("holidays", "holidays1864")

        assertThat(result).isEqualTo("holidays")
    }

    @Test
    fun `given an empty original name, when building the processed name, then the result is null`() {
        val result = MediaFileNameUtils.buildProcessedFileName("", "1864.jpg")

        assertThat(result).isNull()
    }

    @Test
    fun `given a hidden file, when building the processed name, then the leading dot is kept`() {
        val result = MediaFileNameUtils.buildProcessedFileName(".hidden", ".hidden1864.jpg")

        assertThat(result).isEqualTo(".hidden.jpg")
    }

    /* REPLACE EXTENSION */

    @Test
    fun `given a file name with an extension, when replacing it, then the base name is kept`() {
        val result = MediaFileNameUtils.replaceExtension("my-video.mov", "mp4")

        assertThat(result).isEqualTo("my-video.mp4")
    }

    @Test
    fun `given a file name without an extension, when replacing it, then the extension is appended`() {
        val result = MediaFileNameUtils.replaceExtension("my-video", "mp4")

        assertThat(result).isEqualTo("my-video.mp4")
    }

    @Test
    fun `given a file name with several dots, when replacing the extension, then only the last one is replaced`() {
        val result = MediaFileNameUtils.replaceExtension("my.holiday.video.mov", "mp4")

        assertThat(result).isEqualTo("my.holiday.video.mp4")
    }
}
