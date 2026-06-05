package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.store.SiteStore.OnApplicationPasswordCreateError

class OnApplicationPasswordCreateErrorTest {
    @Test
    fun `given a WPAPINetworkError, then errorCode comes from the WP-API error code`() {
        val base = BaseNetworkError(GenericErrorType.SERVER_ERROR, "wp-api error message")
        val wpApiError = WPAPINetworkError(base, "rest_no_route")

        val error = OnApplicationPasswordCreateError(wpApiError, notSupported = false)

        assertThat(error.errorCode).isEqualTo("rest_no_route")
        assertThat(error.message).isEqualTo("wp-api error message")
        assertThat(error.notSupported).isFalse()
    }

    @Test
    fun `given a WPComGsonNetworkError, then errorCode comes from apiError`() {
        val gsonError = WPComGsonNetworkError(
            BaseNetworkError(GenericErrorType.SERVER_ERROR, "wp.com error message")
        ).apply {
            apiError = "application_passwords_disabled"
        }

        val error = OnApplicationPasswordCreateError(gsonError, notSupported = true)

        assertThat(error.errorCode).isEqualTo("application_passwords_disabled")
        assertThat(error.message).isEqualTo("wp.com error message")
        assertThat(error.notSupported).isTrue()
    }

    @Test
    fun `given a plain BaseNetworkError, then errorCode is null and message is captured`() {
        val base = BaseNetworkError(GenericErrorType.NETWORK_ERROR, "offline")

        val error = OnApplicationPasswordCreateError(base, notSupported = false)

        assertThat(error.errorCode).isNull()
        assertThat(error.message).isEqualTo("offline")
        assertThat(error.notSupported).isFalse()
    }
}
