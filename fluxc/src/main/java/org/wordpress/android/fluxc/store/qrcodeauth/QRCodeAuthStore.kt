package org.wordpress.android.fluxc.store.qrcodeauth

import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthRestClient.QRCodeAuthAuthenticateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthRestClient.QRCodeAuthValidateResponse
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeAuthStore
@Inject constructor(
    private val qrcodeAuthRestClient: QRCodeAuthRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun validate(
        data: String,
        token: String
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "validate") {
        val payload = qrcodeAuthRestClient.validate(data, token)
        return@withDefaultContext when {
            payload.isError -> QRCodeAuthResult(payload.error)
            payload.response != null -> QRCodeAuthResult(payload.response.toResult())
            else -> QRCodeAuthResult(QRCodeAuthError(QRCodeAuthErrorType.INVALID_RESPONSE))
        }
    }

    fun QRCodeAuthValidateResponse.toResult() =
        QRCodeAuthValidateResult(browser = this.browser,
            location = this.location,
            success = this.success ?: false
        )

    suspend fun authenticate(
        data: String,
        token: String
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "authenticate") {
        val payload = qrcodeAuthRestClient.authenticate(data, token)
        return@withDefaultContext when {
            payload.isError -> QRCodeAuthResult(payload.error)
            payload.response != null -> QRCodeAuthResult(payload.response.toResult())
            else -> QRCodeAuthResult(QRCodeAuthError(QRCodeAuthErrorType.INVALID_RESPONSE))
        }
    }

    fun QRCodeAuthAuthenticateResponse.toResult() =
        QRCodeAuthAuthenticateResult(authenticated = this.authenticated ?: false)

    data class QRCodeAuthResult<T>(
        val model: T? = null
    ) : Store.OnChanged<QRCodeAuthError>() {
        constructor(error: QRCodeAuthError) : this() {
            this.error = error
        }
    }

    data class QRCodeAuthValidateResult(
        val browser: String? = null,
        val location: String? = null,
        val success: Boolean = false
    )

    data class QRCodeAuthAuthenticateResult(
        val authenticated: Boolean = false
    )
}
