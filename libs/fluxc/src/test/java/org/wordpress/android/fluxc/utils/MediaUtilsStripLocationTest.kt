package org.wordpress.android.fluxc.utils

import androidx.exifinterface.media.ExifInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaUtilsStripLocationTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `stripLocation removes every GPS tag`() {
        val path = copyFixture()
        ExifInterface(path).apply {
            setLatLong(LATITUDE, LONGITUDE)
            setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE, "51/1,30/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, "W")
            setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE, "0/1,7/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, "London")
            setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, "90/1")
            setAttribute(ExifInterface.TAG_GPS_SPEED, "10/1")
            saveAttributes()
        }
        assertThat(ExifInterface(path).latLong).isNotNull
        assertThat(ExifInterface(path).getAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION)).isEqualTo("London")

        MediaUtils.stripLocation(path)

        val stripped = ExifInterface(path)
        assertThat(stripped.latLong).isNull()
        MediaUtils.GPS_TAGS.forEach { tag ->
            assertThat(stripped.getAttribute(tag)).describedAs(tag).isNull()
        }
    }

    @Test
    fun `stripLocation leaves non-GPS tags intact`() {
        val path = copyFixture()
        ExifInterface(path).apply {
            setLatLong(LATITUDE, LONGITUDE)
            setAttribute(ExifInterface.TAG_MAKE, "Pixel")
            saveAttributes()
        }

        MediaUtils.stripLocation(path)

        assertThat(ExifInterface(path).getAttribute(ExifInterface.TAG_MAKE)).isEqualTo("Pixel")
    }

    private fun copyFixture(): String {
        val file = tempFolder.newFile("image.jpg")
        javaClass.classLoader!!.getResourceAsStream("media/strip-location.jpg")!!.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file.absolutePath
    }

    companion object {
        private const val LATITUDE = 51.5072
        private const val LONGITUDE = -0.1276
    }
}
