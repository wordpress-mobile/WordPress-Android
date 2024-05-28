package org.wordpress.android.fluxc.network.rest.wpcom.jetpackai

import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature

sealed class JetpackAIAssistantFeatureResponse {
    data class Success(val model: JetpackAIAssistantFeature) : JetpackAIAssistantFeatureResponse()
    data class Error(
        val type: JetpackAIAssistantFeatureErrorType,
        val message: String? = null
    ) : JetpackAIAssistantFeatureResponse()
}

enum class JetpackAIAssistantFeatureErrorType {
    API_ERROR,
    AUTH_ERROR,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    TIMEOUT,
    NETWORK_ERROR
}
