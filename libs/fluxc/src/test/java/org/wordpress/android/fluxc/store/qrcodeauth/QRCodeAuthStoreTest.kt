package org.wordpress.android.fluxc.store.qrcodeauth

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthPayload
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthRestClient.QRCodeAuthAuthenticateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.qrcodeauth.QRCodeAuthRestClient.QRCodeAuthValidateResponse
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthAuthenticateResult
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthResult
import org.wordpress.android.fluxc.store.qrcodeauth.QRCodeAuthStore.QRCodeAuthValidateResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class QRCodeAuthStoreTest {
    @Mock private lateinit var qrcodeAuthRestClient: QRCodeAuthRestClient
    private lateinit var qrcodeAuthStore: QRCodeAuthStore

    @Before
    fun setUp() {
        qrcodeAuthStore = QRCodeAuthStore(qrcodeAuthRestClient, initCoroutineEngine())
    }

    private val validateResponseSuccess = QRCodeAuthValidateResponse(
        browser = BROWSER,
        location = LOCATION,
        success = SUCCESS
    )

    private val validateResultSuccess = QRCodeAuthValidateResult(
        browser = BROWSER,
        location = LOCATION,
        success = SUCCESS
    )

    private val authenticateResponseSuccess = QRCodeAuthAuthenticateResponse(
        authenticated = AUTHENTICATED
    )

    private val authenticateResultSuccess = QRCodeAuthAuthenticateResult(
        authenticated = AUTHENTICATED
    )

    private val responseError = QRCodeAuthError(
        type = GENERIC_ERROR
    )

    private val resultError = QRCodeAuthError(
        type = GENERIC_ERROR
    )

    @Test
    fun `given success, when validate is triggered, then validate result is returned`() = test {
        whenever(qrcodeAuthRestClient.validate(any(), any())).thenReturn(
            QRCodeAuthPayload(validateResponseSuccess)
        )

        val response = qrcodeAuthStore.validate(DATA_PARAM, TOKEN_PARAM)

        assertNotNull(response.model)
        assertEquals(QRCodeAuthResult(validateResultSuccess), response)
    }

    @Test
    fun `given error, when validate is triggered, then error result is returned`() = test {
        whenever(qrcodeAuthRestClient.validate(any(), any())).thenReturn(
            QRCodeAuthPayload(responseError)
        )

        val response = qrcodeAuthStore.validate(DATA_PARAM, TOKEN_PARAM)

        assertNull(response.model)
        assertEquals(QRCodeAuthResult(resultError), response)
    }

    @Test
    fun `given success, when authenticate is triggered, then authenticate result is returned`() = test {
        whenever(qrcodeAuthRestClient.authenticate(any(), any())).thenReturn(
            QRCodeAuthPayload(authenticateResponseSuccess)
        )

        val response = qrcodeAuthStore.authenticate(DATA_PARAM, TOKEN_PARAM)

        assertNotNull(response.model)
        assertEquals(QRCodeAuthResult(authenticateResultSuccess), response)
    }

    @Test
    fun `given error, when authenticate is triggered, then error result is returned`() = test {
        whenever(qrcodeAuthRestClient.authenticate(any(), any())).thenReturn(
            QRCodeAuthPayload(responseError)
        )

        val response = qrcodeAuthStore.authenticate(DATA_PARAM, TOKEN_PARAM)

        assertNull(response.model)
        assertEquals(QRCodeAuthResult(resultError), response)
    }

    companion object {
        private const val TOKEN_PARAM = "token_param"
        private const val DATA_PARAM = "data_param"
        private const val BROWSER = "Chrome"
        private const val LOCATION = "Secaucus, New Jersey"
        private const val SUCCESS = true
        private const val AUTHENTICATED = true
    }
}
