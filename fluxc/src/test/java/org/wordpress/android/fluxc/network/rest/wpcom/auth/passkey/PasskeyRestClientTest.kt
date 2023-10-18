package org.wordpress.android.fluxc.network.rest.wpcom.auth.passkey

import org.junit.Before
import org.mockito.kotlin.mock

class PasskeyRestClientTest {
    private lateinit var sut: PasskeyRestClient

    @Before
    fun setUp() {
        sut = PasskeyRestClient(
            context = mock(),
            dispatcher = mock(),
            requestQueue = mock(),
            accessToken = mock(),
            userAgent = mock()
        )
    }
}
