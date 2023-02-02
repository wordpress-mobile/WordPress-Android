package org.wordpress.android.fluxc.network.rest.wpapi

sealed interface Nonce {
    val value: String?
        get() = null
    val username: String

    data class Available(override val value: String, override val username: String) : Nonce
    data class FailedRequest(
        val timeOfResponse: Long,
        override val username: String,
        val networkError: WPAPINetworkError? = null,
    ) : Nonce

    data class Unknown(override val username: String) : Nonce
}
