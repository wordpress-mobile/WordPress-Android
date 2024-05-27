package org.wordpress.android.fluxc.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WPComRestClientUtilsTest  {
    companion object {
        private const val LOCALE_PARAM = "locale"
        private const val UNDERSCORE_LOCALE_PARAM = "_locale"
    }

    private val appContext = RuntimeEnvironment.application.applicationContext

    @Test
    fun `getLocaleParamName should return _locale for v2 url`() {
        val url = "https://public-api.wordpress.com/wpcom/v2/something"
        val result = WPComRestClientUtils.getLocaleParamName(url)
        assertEquals(UNDERSCORE_LOCALE_PARAM, result)
    }

    @Test
    fun `getLocaleParamName should return _locale for v3 url`() {
        val url = "https://public-api.wordpress.com/wpcom/v3/something"
        val result = WPComRestClientUtils.getLocaleParamName(url)
        assertEquals(UNDERSCORE_LOCALE_PARAM, result)
    }

    @Test
    fun `getLocaleParamName should return locale for other urls`() {
        val url = "https://public-api.wordpress.com/rest/v1/"
        val result = WPComRestClientUtils.getLocaleParamName(url)
        assertEquals(LOCALE_PARAM, result)
    }

    @Test
    fun `getHttpUrlWithLocale should add correct locale parameter for v2 url`() {
        val url = "https://public-api.wordpress.com/wpcom/v2/something"
        val result = WPComRestClientUtils.getHttpUrlWithLocale(appContext, url)

        assertNotNull(result)
        assertNotNull(result?.queryParameter(UNDERSCORE_LOCALE_PARAM))
    }

    @Test
    fun `getHttpUrlWithLocale should add correct locale parameter for v3 url`() {
        val url = "https://public-api.wordpress.com/wpcom/v3/something"
        val result = WPComRestClientUtils.getHttpUrlWithLocale(appContext, url)

        assertNotNull(result)
        assertNotNull(result?.queryParameter(UNDERSCORE_LOCALE_PARAM))
    }

    @Test
    fun `getHttpUrlWithLocale should add correct locale parameter for other urls`() {
        val url = "https://public-api.wordpress.com/rest/v1/"
        val result = WPComRestClientUtils.getHttpUrlWithLocale(appContext, url)

        assertNotNull(result)
        assertNotNull(result?.queryParameter(LOCALE_PARAM))
    }
}
