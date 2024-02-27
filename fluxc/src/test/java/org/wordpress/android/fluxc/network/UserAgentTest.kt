package org.wordpress.android.fluxc.network

import android.webkit.WebSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.util.PackageUtils
import kotlin.test.assertEquals

private const val APP_NAME = "App Name"
private const val USER_AGENT = "Default User Agent"
private const val APP_VERSION = "1.0"

@RunWith(RobolectricTestRunner::class)
class UserAgentTest {
    private val context = RuntimeEnvironment.application.applicationContext

    @Test
    fun testUserAgent() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use { settings ->
            settings.`when`<Any> { WebSettings.getDefaultUserAgent(context) }.thenReturn(USER_AGENT)
            val result = UserAgent(context, APP_NAME)
            assertEquals("$USER_AGENT $APP_NAME/$APP_VERSION", result.toString())
        }
    }

    @Test
    fun testDefaultUserAgentFailure() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use { settings ->
            settings.`when`<Any> { WebSettings.getDefaultUserAgent(context) }
                .thenThrow(RuntimeException(""))
            val result = UserAgent(context, APP_NAME)
            assertEquals("$APP_NAME/$APP_VERSION", result.toString())
        }
    }

    private fun withMockedPackageUtils(test: () -> Unit) {
        mockStatic(PackageUtils::class.java).use { utils ->
            utils.`when`<Any> { PackageUtils.getVersionName(context) }.thenReturn(APP_VERSION)
            test()
        }
    }
}
