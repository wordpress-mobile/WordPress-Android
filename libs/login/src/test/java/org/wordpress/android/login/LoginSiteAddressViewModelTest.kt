package org.wordpress.android.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import app.cash.turbine.test
import com.sun.jna.Pointer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.login.viewmodel.LoginSiteAddressViewModel
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.AutoDiscoveryAttemptSuccess
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpApiDetails

private val TEST_URL = "https://www.test.com"
private val TEST_URL_AUTH = "https://www.test.com/auth"
private val TEST_URL_AUTH_SUFFIX = "?app_name=android-jetpack-client&success_url=null"

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
 class LoginSiteAddressViewModelTest {
    @Mock
    lateinit var wpLoginClient: WpLoginClient

    @Mock
    lateinit var appSecrets: AppSecrets

    @Mock
    lateinit var wpApiDetails: WpApiDetails

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        wpApiDetails.stub {
            onBlocking {
                findApplicationPasswordsAuthenticationUrl()
            } doReturn TEST_URL_AUTH
        }
    }

    @Test
    fun `GIVEN fresh VM WHEN first state emitted THEN initial values are ok`() = runTest {
        val viewModel = LoginSiteAddressViewModel(wpLoginClient, appSecrets)
        wpLoginClient.stub {
            onBlocking {
                apiDiscovery(eq(TEST_URL))
            } doReturn AutoDiscoveryAttemptSuccess(
                ParsedUrl(Pointer.createConstant(1)),
                ParsedUrl(Pointer.createConstant(1)),
                wpApiDetails
            )
        }

        viewModel.authorizationUrlFlow.test {
            viewModel.runApiDiscovery(TEST_URL)
            skipItems(1) // Skip the initial state
            val state = awaitItem()
            assertEquals("$TEST_URL_AUTH$TEST_URL_AUTH_SUFFIX", state)

            verify(wpLoginClient).apiDiscovery(eq(TEST_URL))
        }
    }
 }
