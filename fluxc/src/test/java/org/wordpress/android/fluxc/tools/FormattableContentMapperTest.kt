package org.wordpress.android.fluxc.tools

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class FormattableContentMapperTest {
    private lateinit var formattableContentMapper: FormattableContentMapper
    private val url = "https://www.wordpress.com"

    @Before
    fun setUp() {
        val gson = ReleaseNetworkModule().provideGson()
        formattableContentMapper = FormattableContentMapper(gson)
    }

    @Test
    fun mapsNotificationSubjectToRichFormattableContent() {
        val notificationSubjectResponse = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/subject-response.json")
        val formattableContent = formattableContentMapper.mapToFormattableContent(notificationSubjectResponse)
        assertEquals("You've received 20 likes on My Site", formattableContent.text)
        assertEquals(2, formattableContent.ranges!!.size)
        with(formattableContent.ranges!![0]) {
            assertEquals(FormattableRangeType.B, this.rangeType())
            assertEquals(listOf(16, 18), this.indices)
        }
        with(formattableContent.ranges!![1]) {
            assertEquals(FormattableRangeType.SITE, this.rangeType())
            assertEquals(123, this.id)
            assertEquals("http://mysite.wordpress.com", this.url)
            assertEquals(listOf(28, 35), this.indices)
        }
    }

    @Test
    fun mapsNotificationBodyToRichFormattableContent() {
        val notificationBodyResponse = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/body-response.json")
        val formattableContent = formattableContentMapper.mapToFormattableContent(notificationBodyResponse)
        assertEquals("This site was created by Author", formattableContent.text)
        assertTrue(formattableContent.meta!!.isMobileButton == true)
        assertEquals(2, formattableContent.ranges!!.size)
        with(formattableContent.ranges!![0]) {
            assertEquals(FormattableRangeType.USER, this.rangeType())
            assertEquals(123, this.siteId)
            assertEquals(111, this.id)
            assertEquals(url, this.url)
            assertEquals(listOf(0, 9), this.indices)
        }
    }

    @Test
    fun mapsScanTypeToScanFormattableRangeType() {
        val notificationBodyResponse = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/body-response.json")
        val formattableContent = formattableContentMapper.mapToFormattableContent(notificationBodyResponse)
        assertEquals(FormattableRangeType.SCAN, formattableContent.ranges!![1].rangeType())
    }

    @Test
    fun mapsActivityLogContentToSimpleFormattableContent() {
        val activityLogBodyResponse = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "activitylog/body-response.json")
        val formattableContent = formattableContentMapper.mapToFormattableContent(activityLogBodyResponse)
        assertEquals("Comment text", formattableContent.text)
        assertEquals(2, formattableContent.ranges!!.size)
        with(formattableContent.ranges!![0]) {
            assertEquals(FormattableRangeType.POST, this.rangeType())
            assertEquals(123, this.siteId)
            assertEquals(111, this.id)
            assertEquals(url, this.url)
            assertEquals("post", this.section)
            assertEquals("edit", this.intent)
            assertEquals("single", this.context)
            assertEquals(listOf(27, 39), this.indices)
        }
    }

    @Test
    fun createsUnknownStringFromNull() {
        val unknownType = FormattableRangeType.fromString(null)

        assertEquals(FormattableRangeType.UNKNOWN, unknownType)
    }

    @Test
    fun createsUnknownStringFromEmptyText() {
        val unknownType = FormattableRangeType.fromString("")

        assertEquals(FormattableRangeType.UNKNOWN, unknownType)
    }

    @Test
    fun mapsJsonArrayToFormattableContentList() {
        val jsonContentArray = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/formattable-content-array.json")
        val formattableList = formattableContentMapper.mapToFormattableContentList(jsonContentArray)
        assertEquals(3, formattableList.size)
        assertEquals("Jennifer Shultz", formattableList[0].text)
        assertEquals("I bought this for my daughter and it fits beautifully!", formattableList[1].text)
        assertEquals("Review for Ninja Hoodie", formattableList[2].text)
    }

    @Test
    fun mapsFormattableContentListToJsonString() {
        val jsonContentArray = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/formattable-content-array.json")
        val formattableList = formattableContentMapper.mapToFormattableContentList(jsonContentArray)
        val formattableJson = formattableContentMapper.mapFormattableContentListToJson(formattableList)
        assertEquals(jsonContentArray, formattableJson)
    }

    @Test
    fun mapsRewindDownloadReadyTypeToRewindDownloadReadyFormattableRangeType() {
        val response = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/rewind-download-ready.json")
        val formattableContent = formattableContentMapper.mapToFormattableContent(response)
        assertEquals(FormattableRangeType.REWIND_DOWNLOAD_READY, formattableContent.ranges!![0].rangeType())
    }

    @Test
    fun `getting ID from FormattableRange returns correct value depending on value type `() {
        val notificationCommentBodyResponse = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "notifications/comment-response.json")
        val formattableContent = formattableContentMapper.mapToFormattableContent(notificationCommentBodyResponse)
        assertEquals(4, formattableContent.ranges!!.size)
        // ID is missing
        with(formattableContent.ranges!![0]) {
            assertEquals(null, this.id)
        }
        // ID is numerical
        with(formattableContent.ranges!![1]) {
            assertEquals(16, this.id)
        }
        // ID is non-numerical
        with(formattableContent.ranges!![2]) {
            assertEquals(null, this.id)
        }
    }
}
