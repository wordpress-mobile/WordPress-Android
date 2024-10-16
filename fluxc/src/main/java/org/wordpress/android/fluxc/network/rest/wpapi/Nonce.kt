package org.wordpress.android.fluxc.network.rest.wpapi

sealed interface Nonce {
    val value: String?
        get() = null
    val username: String?

    data class Available(override val value: String, override val username: String) : Nonce
    data class FailedRequest(
        val timeOfResponse: Long,
        override val username: String,
        val type: CookieNonceErrorType,
        val networkError: WPAPINetworkError? = null,
        val errorMessage: String? = null,
    ) : Nonce

    data class Unknown(override val username: String?) : Nonce

    enum class CookieNonceErrorType {
        NOT_AUTHENTICATED,
        INVALID_RESPONSE,
        INVALID_CREDENTIALS,
        CUSTOM_LOGIN_URL,
        CUSTOM_ADMIN_URL,
        INVALID_NONCE,
        GENERIC_ERROR,
        UNKNOWN
    }
}
