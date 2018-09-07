package org.wordpress.android.fluxc.tools

import com.google.gson.Gson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class FormattableContentMapperTest {
    private lateinit var formattableContentMapper: FormattableContentMapper
    private val url = "https://www.wordpress.com"
    private val notificationBodyResponse: String = "{\n" +
            "          \"text\": \"This site was created by Author\",\n" +
            "          \"ranges\": [\n" +
            "            {\n" +
            "              \"email\": \"user@automattic.com\",\n" +
            "              \"url\": \"$url\",\n" +
            "              \"id\": 111,\n" +
            "              \"site_id\": 123,\n" +
            "              \"type\": \"user\",\n" +
            "              \"indices\": [\n" +
            "                0,\n" +
            "                9\n" +
            "              ]\n" +
            "            }\n" +
            "          ],\n" +
            "          \"media\": [\n" +
            "            {\n" +
            "              \"type\": \"image\",\n" +
            "              \"indices\": [\n" +
            "                0,\n" +
            "                0\n" +
            "              ],\n" +
            "              \"height\": \"256\",\n" +
            "              \"width\": \"256\",\n" +
            "              \"url\": \"https://gravatar.jpg\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"actions\": {\n" +
            "            \"follow\": false\n" +
            "          },\n" +
            "          \"meta\": {\n" +
            "            \"links\": {\n" +
            "              \"email\": \"user@wp.com\",\n" +
            "              \"home\": \"https://user.blog\"\n" +
            "            },\n" +
            "            \"ids\": {\n" +
            "              \"user\": 1,\n" +
            "              \"site\": 2\n" +
            "            },\n" +
            "            \"titles\": {\n" +
            "              \"home\": \"Title\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"type\": \"user\"\n" +
            "        }"

    private val activityLogBodyResponse = "{\n" +
            "          \"text\": \"Comment text\",\n" +
            "          \"ranges\": [\n" +
            "            {\n" +
            "              \"url\": \"$url\",\n" +
            "              \"indices\": [\n" +
            "                27,\n" +
            "                39\n" +
            "              ],\n" +
            "              \"site_id\": 123,\n" +
            "              \"section\": \"post\",\n" +
            "              \"intent\": \"edit\",\n" +
            "              \"context\": \"single\",\n" +
            "              \"id\": 111\n" +
            "            },\n" +
            "            {\n" +
            "              \"url\": \"$url\",\n" +
            "              \"indices\": [\n" +
            "                0,\n" +
            "                7\n" +
            "              ],\n" +
            "              \"site_id\": 123,\n" +
            "              \"section\": \"comment\",\n" +
            "              \"intent\": \"edit\",\n" +
            "              \"context\": \"single\",\n" +
            "              \"id\": 17,\n" +
            "              \"root_id\": 68\n" +
            "            }\n" +
            "          ]\n" +
            "        }"

    @Before
    fun setUp() {
        val gson = Gson()
        formattableContentMapper = FormattableContentMapper(gson)
    }

    @Test
    fun mapsNotificationToRichFormattableContent() {
        val formattableContent = formattableContentMapper.mapToFormattableContent(notificationBodyResponse)
        assertEquals("This site was created by Author", formattableContent.text)
        assertEquals(1, formattableContent.ranges!!.size)
        with(formattableContent.ranges!![0]) {
            assertEquals(FormattableRangeType.USER, this.rangeType())
            assertEquals(123, this.siteId)
            assertEquals(111, this.id)
            assertEquals(url, this.url)
            assertEquals(listOf(0, 9), this.indices)
        }
    }

    @Test
    fun mapsActivityLogContentToSimpleFormattableContent() {
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
}
