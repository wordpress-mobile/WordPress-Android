package org.wordpress.android.fluxc.network

import android.webkit.WebSettings
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.util.PackageUtils
import kotlin.test.assertEquals

private const val APP_NAME = "App Name"
private const val USER_AGENT = "Default User Agent"
private const val APP_VERSION = "1.0"

@RunWith(RobolectricTestRunner::class)
class UserAgentTest {
    private val context = RuntimeEnvironment.getApplication().applicationContext

    @Test
    fun testUserAgent() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use {
            whenever(WebSettings.getDefaultUserAgent(context)).thenReturn(USER_AGENT)
            // Use the Unconfined dispatcher to allow the test to run synchronously
            val result = UserAgent(context, APP_NAME, bgDispatcher = Dispatchers.Unconfined)
            assertEquals("$USER_AGENT $APP_NAME/$APP_VERSION", result.toString())
        }
    }

    @Test
    fun testDefaultUserAgentFailure() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use {
            whenever(WebSettings.getDefaultUserAgent(context)).thenThrow(RuntimeException(""))
            // Use the Unconfined dispatcher to allow the test to run synchronously
            val result = UserAgent(context, APP_NAME, bgDispatcher = Dispatchers.Unconfined)
            assertEquals("$APP_NAME/$APP_VERSION", result.toString())
        }
    }

    private fun withMockedPackageUtils(test: () -> Unit) {
        mockStatic(PackageUtils::class.java).use { utils ->
            whenever(PackageUtils.getVersionName(context)).thenReturn(APP_VERSION)
            test()
        }
    }
}
