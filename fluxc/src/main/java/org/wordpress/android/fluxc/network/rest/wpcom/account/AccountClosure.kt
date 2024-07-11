package org.wordpress.android.fluxc.network.rest.wpcom.account

import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.Success
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.Failure
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.Error
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType

/**
 * Performs an HTTP POST call to v1.1 /me/account/close endpoint to close the user account.
 */
fun AccountRestClient.closeAccount(onResult: (CloseAccountResult) -> Unit) {
    add(WPComGsonRequest.buildPostRequest(
        WPCOMREST.me.account.close.urlV1_1,
        null,
        CloseAccountResponse::class.java,
        { onResult(Success) },
        { error ->
            val errorType = ErrorType.values().firstOrNull { error.apiError == it.token } ?: ErrorType.UNKNOWN
            onResult(Failure(error = Error(errorType, error.message)))
        },

    ))
}

class CloseAccountResponse: Response

sealed class CloseAccountResult {
    object Success: CloseAccountResult()
    data class Failure(val error: Error): CloseAccountResult()
    data class Error(val errorType: ErrorType, val message: String)
    enum class ErrorType(val token: String? = null) {
        UNAUTHORIZED("unauthorized"),
        ATOMIC_SITE("atomic-site"),
        CHARGEBACKED_SITE("chargebacked-site"),
        ACTIVE_SUBSCRIPTIONS("active-subscriptions"),
        ACTIVE_MEMBERSHIPS("active-memberships"),
        INVALID_TOKEN("invalid_token"),
        UNKNOWN,
    }
}
