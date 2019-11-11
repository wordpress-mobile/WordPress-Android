package org.wordpress.android.ui.posts.reactnative

import org.junit.Assert.assertEquals
import org.junit.Test

private const val INPUT_PATH = "/wp/v2/media/54?context=edit&_locale=user"
private val INPUT_PATH_PARAM_MAP = mapOf("context" to "edit", "_locale" to "user")

class ReactNativeUrlHandlerTest {
    @Test
    fun `successfully generates url and query params for WPcom`() {
        val siteId = 555L

        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/$siteId/media/54"
        val expected = Pair(expectedUrl, INPUT_PATH_PARAM_MAP)

        assertEquals(expected, parseUrlAndParamsForWPCom(INPUT_PATH, siteId))
    }

    @Test
    fun `successfully generates url and query params for WPorg`() {
        val siteUrl = "https://jurassic.ninja"

        val expectedUrl = "$siteUrl/wp-json/wp/v2/media/54"
        val expectedParams = INPUT_PATH_PARAM_MAP.plus("context" to "view")
        val expected = Pair(expectedUrl, expectedParams)

        assertEquals(expected, parseUrlAndParamsForWPOrg(INPUT_PATH, siteUrl))
    }
}
