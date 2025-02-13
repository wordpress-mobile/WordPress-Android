package org.wordpress.android.fluxc.network

import android.webkit.WebSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            CoroutineScope(Dispatchers.Default).launch {
                whenever(WebSettings.getDefaultUserAgent(context)).thenReturn(USER_AGENT)
                // we need to delay here to give `getDefaultUserAgent()` time since it runs on a separate thread
                delay(500)
                val result = UserAgent(context, APP_NAME)
                assertEquals("$USER_AGENT $APP_NAME/$APP_VERSION", result.toString())
            }
        }
    }

    @Test
    fun testDefaultUserAgentFailure() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use {
            whenever(WebSettings.getDefaultUserAgent(context)).thenThrow(RuntimeException(""))
            val result = UserAgent(context, APP_NAME)
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
