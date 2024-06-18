package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

sealed class JetpackAIQueryResponse {
    data class Success(val model: String?, val choices: List<Choice>) : JetpackAIQueryResponse() {
        data class Choice(val index: Int?, val message: Message?) {
            data class Message(val role: String?, val content: String?)
        }
    }

    data class Error(
        val type: JetpackAIQueryErrorType,
        val message: String? = null
    ) : JetpackAIQueryResponse()
}

enum class JetpackAIQueryErrorType {
    API_ERROR,
    AUTH_ERROR,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    TIMEOUT,
    NETWORK_ERROR,
    INVALID_DATA
}
