package org.wordpress.android.fluxc.network.rest.wpapi

sealed class Nonce(open val value: String?) {
    data class Available(override val value: String) : Nonce(value)
    data class FailedRequest(
        val timeOfResponse: Long,
        val networkError: WPAPINetworkError? = null
    ) : Nonce(null)

    object Unknown : Nonce(null)
}
