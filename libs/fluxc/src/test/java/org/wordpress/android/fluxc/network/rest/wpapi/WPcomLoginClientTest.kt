package org.wordpress.android.fluxc.network.rest.wpapi

import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.test
import java.io.IOException
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WPcomLoginClientTest {
    private val appSecrets = AppSecrets("appId", "appSecret", "https://redirect.example/cb")

    @Test
    fun `IOException during token exchange returns NetworkError with cause`() = test {
        val ioException = IOException("simulated network failure")
        val throwingInterceptor = Interceptor { _ -> throw ioException }

        val subject = WPcomLoginClient(
            context = Dispatchers.Unconfined,
            appSecrets = appSecrets,
            interceptors = setOf(throwingInterceptor)
        )

        val result = subject.exchangeAuthCodeForToken("any_code")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<WPcomLoginError.NetworkError>(error)
        assertSame(ioException, error.cause)
    }
}
