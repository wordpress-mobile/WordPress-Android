package org.wordpress.android.fluxc.store.qrcode

import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient.QrcodeAuthenticateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient.QrcodeValidateResponse
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrcodeStore
@Inject constructor(
    private val qrcodeRestClient: QrcodeRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun validate(
        data: String,
        token: String
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "validate") {
        val payload = qrcodeRestClient.validate(data, token)
        return@withDefaultContext when {
            payload.isError -> QrcodeResult(payload.error)
            payload.response != null -> QrcodeResult(payload.response.toResult())
            else -> QrcodeResult(QrcodeError(QrcodeErrorType.INVALID_RESPONSE))
        }
    }

    fun QrcodeValidateResponse.toResult() =
        QrcodeValidateResult(browser = this.browser,
            location = this.location,
            success = this.success ?: false
        )

    suspend fun authenticate(
        data: String,
        token: String
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "authenticate") {
        val payload = qrcodeRestClient.authenticate(data, token)
        return@withDefaultContext when {
            payload.isError -> QrcodeResult(payload.error)
            payload.response != null -> QrcodeResult(payload.response.toResult())
            else -> QrcodeResult(QrcodeError(QrcodeErrorType.INVALID_RESPONSE))
        }
    }

    fun QrcodeAuthenticateResponse.toResult() =
        QrcodeAuthenticateResult(authenticated = this.authenticated ?: false)

    data class QrcodeResult<T>(
        val model: T? = null
    ) : Store.OnChanged<QrcodeError>() {
        constructor(error: QrcodeError) : this() {
            this.error = error
        }
    }

    data class QrcodeValidateResult(
        val browser: String? = null,
        val location: String? = null,
        val success: Boolean = false
    )

    data class QrcodeAuthenticateResult(
        val authenticated: Boolean = false
    )
}
