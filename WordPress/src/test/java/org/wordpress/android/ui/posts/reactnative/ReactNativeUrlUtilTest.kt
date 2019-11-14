package org.wordpress.android.ui.posts.reactnative

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.TestApplication

private const val INPUT_PATH = "/wp/v2/media/54"
private const val INPUT_QUERY_PARAMS = "?context=edit&_locale=user"
private val INPUT_QUERY_PARAMS_MAP = mapOf("context" to "edit", "_locale" to "user")

@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.LOLLIPOP])
@RunWith(RobolectricTestRunner::class)
class ReactNativeUrlUtilTest {
    private lateinit var subject: ReactNativeUrlUtil

    @Before
    fun setUp() {
        subject = ReactNativeUrlUtil()
    }

    @Test
    fun `successfully generates url and query params for WPcom`() {
        val siteId = 555L

        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/$siteId/media/54"
        val expected = Pair(expectedUrl, INPUT_QUERY_PARAMS_MAP)

        assertEquals(expected, subject.parseUrlAndParamsForWPCom(INPUT_PATH + INPUT_QUERY_PARAMS, siteId))
    }

    @Test
    fun `successfully generates url missing query params for WPcom`() {
        val siteId = 555L

        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/$siteId/media/54"
        val expected = Pair(expectedUrl, emptyMap<String, String>())

        assertEquals(expected, subject.parseUrlAndParamsForWPCom(INPUT_PATH, siteId))
    }

    @Test
    fun `successfully generates url and query params for WPorg`() {
        val siteUrl = "https://jurassic.ninja"

        val expectedUrl = "$siteUrl/wp-json/wp/v2/media/54"

        // changes context from edit to view
        val expectedParams = INPUT_QUERY_PARAMS_MAP.plus("context" to "view")

        val expected = Pair(expectedUrl, expectedParams)
        assertEquals(expected, subject.parseUrlAndParamsForWPOrg(INPUT_PATH + INPUT_QUERY_PARAMS, siteUrl))
    }

    @Test
    fun `successfully generates url missing query params for WPorg`() {
        val siteUrl = "https://jurassic.ninja"

        val expectedUrl = "$siteUrl/wp-json/wp/v2/media/54"
        val expected = Pair(expectedUrl, emptyMap<String, String>())

        assertEquals(expected, subject.parseUrlAndParamsForWPOrg(INPUT_PATH, siteUrl))
    }
}
