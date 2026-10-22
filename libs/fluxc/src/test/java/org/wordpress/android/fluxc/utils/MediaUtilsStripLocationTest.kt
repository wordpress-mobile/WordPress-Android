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
    fun `stripLocation removes every GPS tag and keeps other tags`() {
        val path = copyFixture()
        ExifInterface(path).apply {
            GPS_TAG_VALUES.forEach { (tag, value) -> setAttribute(tag, value) }
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_MAKE, MAKE)
            saveAttributes()
        }
        val seeded = ExifInterface(path)
        GPS_TAG_VALUES.keys.forEach { tag ->
            assertThat(seeded.getAttribute(tag)).describedAs("seeded $tag").isNotNull
        }

        MediaUtils.stripLocation(path)

        val stripped = ExifInterface(path)
        assertThat(stripped.latLong).isNull()
        GPS_TAG_VALUES.keys.forEach { tag ->
            assertThat(stripped.getAttribute(tag)).describedAs(tag).isNull()
        }
        assertThat(stripped.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED))
            .isEqualTo(ExifInterface.ORIENTATION_ROTATE_90)
        assertThat(stripped.getAttribute(ExifInterface.TAG_MAKE)).isEqualTo(MAKE)
    }

    private fun copyFixture(): String {
        val file = tempFolder.newFile("image.jpg")
        javaClass.classLoader!!.getResourceAsStream("media/strip-location.jpg")!!.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file.absolutePath
    }

    companion object {
        private const val MAKE = "Google"

        // Kept independent of ExifUtils.GPS_TAGS so a tag dropped from that list fails this test
        private val GPS_TAG_VALUES = mapOf(
            ExifInterface.TAG_GPS_VERSION_ID to "2 3 0 0",
            ExifInterface.TAG_GPS_LATITUDE_REF to "N",
            ExifInterface.TAG_GPS_LATITUDE to "51/1,30/1,25/1",
            ExifInterface.TAG_GPS_LONGITUDE_REF to "W",
            ExifInterface.TAG_GPS_LONGITUDE to "0/1,7/1,39/1",
            ExifInterface.TAG_GPS_ALTITUDE_REF to "0",
            ExifInterface.TAG_GPS_ALTITUDE to "35/1",
            ExifInterface.TAG_GPS_TIMESTAMP to "12:30:00",
            ExifInterface.TAG_GPS_SATELLITES to "8",
            ExifInterface.TAG_GPS_STATUS to "A",
            ExifInterface.TAG_GPS_MEASURE_MODE to "3",
            ExifInterface.TAG_GPS_DOP to "1/1",
            ExifInterface.TAG_GPS_SPEED_REF to "K",
            ExifInterface.TAG_GPS_SPEED to "10/1",
            ExifInterface.TAG_GPS_TRACK_REF to "T",
            ExifInterface.TAG_GPS_TRACK to "90/1",
            ExifInterface.TAG_GPS_IMG_DIRECTION_REF to "T",
            ExifInterface.TAG_GPS_IMG_DIRECTION to "90/1",
            ExifInterface.TAG_GPS_MAP_DATUM to "WGS-84",
            ExifInterface.TAG_GPS_DEST_LATITUDE_REF to "N",
            ExifInterface.TAG_GPS_DEST_LATITUDE to "48/1,51/1,24/1",
            ExifInterface.TAG_GPS_DEST_LONGITUDE_REF to "E",
            ExifInterface.TAG_GPS_DEST_LONGITUDE to "2/1,21/1,8/1",
            ExifInterface.TAG_GPS_DEST_BEARING_REF to "T",
            ExifInterface.TAG_GPS_DEST_BEARING to "45/1",
            ExifInterface.TAG_GPS_DEST_DISTANCE_REF to "K",
            ExifInterface.TAG_GPS_DEST_DISTANCE to "340/1",
            ExifInterface.TAG_GPS_PROCESSING_METHOD to "GPS",
            ExifInterface.TAG_GPS_AREA_INFORMATION to "London",
            ExifInterface.TAG_GPS_DATESTAMP to "2026:10:22",
            ExifInterface.TAG_GPS_DIFFERENTIAL to "0",
            ExifInterface.TAG_GPS_H_POSITIONING_ERROR to "5/1"
        )
    }
}
