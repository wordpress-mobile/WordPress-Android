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
            GPS_TAG_VALUES.forEach { (tag, value) -> setAttribute(tag, value) }
            saveAttributes()
        }

        MediaUtils.stripLocation(path)

        val stripped = ExifInterface(path)
        assertThat(stripped.latLong).isNull()
        GPS_TAG_VALUES.keys.forEach { tag ->
            assertThat(stripped.getAttribute(tag)).describedAs(tag).isNull()
        }
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
        private val GPS_TAG_VALUES = mapOf(
            ExifInterface.TAG_GPS_DEST_LATITUDE_REF to "N",
            ExifInterface.TAG_GPS_DEST_LATITUDE to "51/1,30/1,0/1",
            ExifInterface.TAG_GPS_DEST_LONGITUDE_REF to "W",
            ExifInterface.TAG_GPS_DEST_LONGITUDE to "0/1,7/1,0/1",
            ExifInterface.TAG_GPS_AREA_INFORMATION to "London",
            ExifInterface.TAG_GPS_IMG_DIRECTION to "90/1",
            ExifInterface.TAG_GPS_SPEED to "10/1"
        )
    }
}
