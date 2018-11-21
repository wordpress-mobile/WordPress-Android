package org.wordpress.android.fluxc.tools

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
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

    private val jsonContentArray = "[{\"media\":[{\"height\":\"256\",\"width\":\"256\",\"type\":\"image\",\"url\":\"https://2.gravatar.com/avatar/ebab642c3eb6022e6986f9dcf3147c1e?s\\u003d256\\u0026d\\u003dhttps%3A%2F%2Fsecure.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D256\\u0026r\\u003dG\",\"indices\":[0,0]}],\"meta\":{\"links\":{\"email\":\"jshultz@test.com\"}},\"text\":\"Jennifer Shultz\",\"type\":\"user\",\"ranges\":[{\"type\":\"user\",\"indices\":[0,15]}]},{\"actions\":{\"spam-comment\":false,\"trash-comment\":false,\"approve-comment\":false,\"edit-comment\":true,\"replyto-comment\":true},\"meta\":{\"ids\":{\"site\":153482281,\"comment\":2716,\"post\":2231},\"links\":{\"site\":\"https://public-api.wordpress.com/rest/v1/sites/153482281\",\"comment\":\"https://public-api.wordpress.com/rest/v1/comments/2716\",\"post\":\"https://public-api.wordpress.com/rest/v1/posts/2231\"}},\"text\":\"I bought this for my daughter and it fits beautifully!\",\"type\":\"comment\",\"nest_level\":0},{\"text\":\"Review for Ninja Hoodie\",\"ranges\":[{\"type\":\"link\",\"url\":\"https://testwooshop.mystagingwebsite.com/product/ninja-hoodie/\",\"indices\":[11,23]}]}]"

    @Before
    fun setUp() {
        val gson = ReleaseNetworkModule().provideGson()
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

    @Test
    fun mapsJsonArrayToFormattableContentList() {
        val formattableList = formattableContentMapper.mapToFormattableContentList(jsonContentArray)
        assertEquals(3, formattableList.size)
        assertEquals("Jennifer Shultz", formattableList[0].text)
        assertEquals("I bought this for my daughter and it fits beautifully!", formattableList[1].text)
        assertEquals("Review for Ninja Hoodie", formattableList[2].text)
    }

    @Test
    fun mapsFormattableContentListToJsonString() {
        val formattableList = formattableContentMapper.mapToFormattableContentList(jsonContentArray)
        val formattableJson = formattableContentMapper.mapFormattableContentListToJson(formattableList)
        assertEquals(jsonContentArray, formattableJson)
    }
}
