package org.wordpress.android.fluxc.store.qrcode

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeError
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodePayload
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient.QrcodeAuthenticateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.qrcode.QrcodeRestClient.QrcodeValidateResponse
import org.wordpress.android.fluxc.store.qrcode.QrcodeStore.QrcodeAuthenticateResult
import org.wordpress.android.fluxc.store.qrcode.QrcodeStore.QrcodeResult
import org.wordpress.android.fluxc.store.qrcode.QrcodeStore.QrcodeValidateResult
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class QrcodeStoreTest {
    @Mock private lateinit var qrcodeRestClient: QrcodeRestClient
    private lateinit var qrcodeStore: QrcodeStore

    @Before
    fun setUp() {
        qrcodeStore = QrcodeStore(qrcodeRestClient, initCoroutineEngine())
    }

    private val validateResponseSuccess = QrcodeValidateResponse(
        browser = BROWSER,
        location = LOCATION,
        success = SUCCESS
    )

    private val validateResultSuccess = QrcodeValidateResult(
        browser = BROWSER,
        location = LOCATION,
        success = SUCCESS
    )

    private val authenticateResponseSuccess = QrcodeAuthenticateResponse(
        authenticated = AUTHENTICATED
    )

    private val authenticateResultSuccess = QrcodeAuthenticateResult(
        authenticated = AUTHENTICATED
    )

    private val responseError = QrcodeError(
        type = GENERIC_ERROR
    )

    private val resultError = QrcodeError(
        type = GENERIC_ERROR
    )

    @Test
    fun `given success, when validate is triggered, then validate result is returned`() = test {
        whenever(qrcodeRestClient.validate(any(), any())).thenReturn(
            QrcodePayload(validateResponseSuccess)
        )

        val response = qrcodeStore.validate(DATA_PARAM, TOKEN_PARAM)

        assertNotNull(response.model)
        assertEquals(QrcodeResult(validateResultSuccess), response)
    }

    @Test
    fun `given error, when validate is triggered, then error result is returned`() = test {
        whenever(qrcodeRestClient.validate(any(), any())).thenReturn(
            QrcodePayload(responseError)
        )

        val response = qrcodeStore.validate(DATA_PARAM, TOKEN_PARAM)

        assertNull(response.model)
        assertEquals(QrcodeResult(resultError), response)
    }

    @Test
    fun `given success, when authenticate is triggered, then authenticate result is returned`() = test {
        whenever(qrcodeRestClient.authenticate(any(), any())).thenReturn(
            QrcodePayload(authenticateResponseSuccess)
        )

        val response = qrcodeStore.authenticate(DATA_PARAM, TOKEN_PARAM)

        assertNotNull(response.model)
        assertEquals(QrcodeResult(authenticateResultSuccess), response)
    }

    @Test
    fun `given error, when authenticate is triggered, then error result is returned`() = test {
        whenever(qrcodeRestClient.authenticate(any(), any())).thenReturn(
            QrcodePayload(responseError)
        )

        val response = qrcodeStore.authenticate(DATA_PARAM, TOKEN_PARAM)

        assertNull(response.model)
        assertEquals(QrcodeResult(resultError), response)
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
