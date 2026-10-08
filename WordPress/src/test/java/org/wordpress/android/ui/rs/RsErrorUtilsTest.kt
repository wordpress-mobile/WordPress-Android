package org.wordpress.android.ui.rs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode

class RsErrorUtilsTest {
    private val resourceProvider: ResourceProvider = mock()
    private val networkUtilsWrapper: NetworkUtilsWrapper = mock()

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(any())).thenAnswer { "res:${it.getArgument<Int>(0)}" }
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `a failed lookup while online is reported as a network error`() {
        val e = RsBridgeException(
            "Failed to fetch post",
            reason = RequestExecutionErrorReason.NonExistentSiteError(
                errorMessage = "Unable to resolve host",
                suggestedAction = "Check the URL"
            )
        )

        assertThat(message(e)).isEqualTo("res:${R.string.error_generic_network}")
    }

    @Test
    fun `a rejected credential from the bridge is reported as an auth error`() {
        val e = RsBridgeException("Unauthorized", errorCode = WpErrorCode.Unauthorized())

        assertThat(message(e)).isEqualTo("res:${R.string.post_rs_error_auth}")
    }

    @Test
    fun `any other bridge failure falls back to the caller's message`() {
        val e = RsBridgeException("Invalid param", errorCode = WpErrorCode.InvalidParam())

        assertThat(message(e)).isEqualTo("res:${R.string.post_not_found}")
    }

    private fun message(e: Exception) = RsErrorUtils.friendlyErrorMessage(
        e, R.string.post_not_found, resourceProvider, networkUtilsWrapper
    )
}
