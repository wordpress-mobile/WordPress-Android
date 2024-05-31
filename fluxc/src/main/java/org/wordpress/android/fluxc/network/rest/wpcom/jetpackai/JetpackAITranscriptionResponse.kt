package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

sealed class JetpackAITranscriptionResponse {
    data class Success(val model: String) : JetpackAITranscriptionResponse()
    data class Error(
        val type: JetpackAITranscriptionErrorType,
        val message: String? = null
    ) : JetpackAITranscriptionResponse()
}


enum class JetpackAITranscriptionErrorType {
    API_ERROR,
    AUTH_ERROR,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    TIMEOUT,
    NETWORK_ERROR,
    CONNECTION_ERROR,
    // local errors
    INELIGIBLE_AUDIO_FILE,
    PARSE_ERROR,
    // HTTP
    BAD_REQUEST,
    NOT_FOUND,
    NOT_AUTHENTICATED,
    REQUEST_TOO_LARGE,
    SERVER_ERROR,
    TOO_MANY_REQUESTS,
    JETPACK_AI_SERVICE_UNAVAILABLE
}
