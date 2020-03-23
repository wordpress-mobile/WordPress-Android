package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

sealed class Nonce(open val value: String?) {
    data class Available(override val value: String) : Nonce(value)
    data class FailedRequest(val timeOfResponse: Long) : Nonce(null)
    object Unknown : Nonce(null)
}
