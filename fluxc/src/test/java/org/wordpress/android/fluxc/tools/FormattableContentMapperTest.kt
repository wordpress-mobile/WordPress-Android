package org.wordpress.android.fluxc.tools

import com.google.gson.Gson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class FormattableContentMapperTest {
    lateinit var formattableContentMapper: FormattableContentMapper
    val serverResponse: String = "{\n" +
            "  \"text\": \"This site was created by Author\", \n" +
            "  \"type\": \"site\",  \n" +
            "  \"actions\": {\n" +
            "      \"follow\": false  \n" +
            "  },\n" +
            "  \"ranges\": [  \n" +
            "      {\n" +
            "         \"type\": \"site\",  \n" +
            "         \"siteID\": 123,  \n" +
            "         \"url\": \"https://www.wordpress.com\", \n" +
            "         \"indices\": [  \n" +
            "            0,\n" +
            "            9\n" +
            "          ]\n" +
            "      },\n" +
            "      {\n" +
            "         \"type\": \"user\",\n" +
            "         \"indices\": [\n" +
            "            25,\n" +
            "            31\n" +
            "          ]\n" +
            "      }\n" +
            "  ],\n" +
            "  \"meta\": {\n" +
            "      \n" +
            "  }\n" +
            "}"

    @Before
    fun setUp() {
        val gson = Gson()
        formattableContentMapper = FormattableContentMapper(gson)
    }

    @Test
    fun verifyJsonToObjectMapping() {
        val formattableContent = formattableContentMapper.mapToFormattableContent(serverResponse)
        assertEquals("This site was created by Author", formattableContent.text)
        assertEquals(2, formattableContent.ranges!!.size)
        with(formattableContent.ranges!![0]) {
            assertEquals(FormattableRangeType.SITE, this.rangeType)
            assertEquals(123, this.siteId)
            assertEquals("https://www.wordpress.com", this.url)
            assertEquals(listOf(0, 9), this.indices)
        }
    }
}
