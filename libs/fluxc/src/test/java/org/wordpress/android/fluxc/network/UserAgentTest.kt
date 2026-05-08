package org.wordpress.android.fluxc.network

import android.webkit.WebSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.util.PackageUtils
import java.util.concurrent.Executor
import kotlin.test.assertEquals

private const val APP_NAME = "App Name"
private const val USER_AGENT = "Default User Agent"
private const val APP_VERSION = "1.0"

@RunWith(RobolectricTestRunner::class)
class UserAgentTest {
    private val context = RuntimeEnvironment.getApplication().applicationContext

    // Run eager-load work synchronously on the calling thread so static mocks
    // (which are thread-local in Mockito 5+) remain in effect.
    private val syncExecutor = Executor(Runnable::run)

    @Test
    fun testUserAgent() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use {
            whenever(WebSettings.getDefaultUserAgent(context)).thenReturn(USER_AGENT)
            val result = UserAgent(context, APP_NAME, syncExecutor)
            assertEquals("$USER_AGENT $APP_NAME/$APP_VERSION", result.webViewUserAgent)
        }
    }

    @Test
    fun testDefaultUserAgentFailure() = withMockedPackageUtils {
        mockStatic(WebSettings::class.java).use {
            whenever(WebSettings.getDefaultUserAgent(context)).thenThrow(RuntimeException(""))
            val result = UserAgent(context, APP_NAME, syncExecutor)
            assertEquals("$APP_NAME/$APP_VERSION", result.webViewUserAgent)
        }
    }

    private fun withMockedPackageUtils(test: () -> Unit) {
        mockStatic(PackageUtils::class.java).use { utils ->
            whenever(PackageUtils.getVersionName(context)).thenReturn(APP_VERSION)
            test()
        }
    }
}
