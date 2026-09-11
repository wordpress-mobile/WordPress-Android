package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.usecases.DomainContactField
import org.wordpress.android.ui.domains.usecases.RedeemCartResult
import org.wordpress.android.ui.domains.usecases.RedeemCartUseCase
import org.wordpress.android.ui.domains.usecases.redeemCartParams
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CartKey
import uniffi.wp_api.DomainContactInformation
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.TransactionPaymentMethod
import uniffi.wp_api.WpErrorCode

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class RedeemCartUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: RedeemCartUseCase

    private val cart = testShoppingCart()

    private val contact = DomainContactInformation(
        firstName = "John",
        lastName = "Smith",
        organization = null,
        address1 = "Street 1",
        address2 = null,
        postalCode = "10018",
        city = "First City",
        state = "CA",
        countryCode = "US",
        email = "email@wordpress.org",
        phone = "+1.3124567890",
        fax = null,
        extra = null,
    )

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = RedeemCartUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given the cart is redeemed, when execute, returns success`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(testTransactionReceipt()) as WpRequestResult<Any>)

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(RedeemCartResult.Success)
    }

    @Test
    fun `given the request fails, when execute, returns error carrying no field or message`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.POST
                )
            )

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(RedeemCartResult.Error())
    }

    @Test
    fun `given a field is rejected, when execute, the error names it`() = test {
        whenever(wpComApiClient.request<Any>(any())).thenReturn(wpError("phone", "Wrong phone number"))

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(
            RedeemCartResult.Error(field = DomainContactField.PHONE, message = "Wrong phone number")
        )
    }

    @Test
    fun `given a rejection that names no form field, when execute, the error carries the message alone`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(wpError("insufficient_funds", "Not enough credits"))

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(RedeemCartResult.Error(field = null, message = "Not enough credits"))
    }

    @Test
    fun `given the device is offline, when execute, the error says so`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.RequestExecutionFailed<Any>(
                    null,
                    null,
                    RequestExecutionErrorReason.DeviceIsOfflineError("No internet connection"),
                    "",
                    RequestMethod.POST,
                )
            )

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(RedeemCartResult.Error(isDeviceOffline = true))
    }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(RedeemCartResult.Error())
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute(cart, contact)

        assertThat(result).isEqualTo(RedeemCartResult.Error())
    }

    @Test
    fun `the request pays with credits and carries the cart and contact unchanged`() {
        val params = redeemCartParams(testShoppingCart(CartKey.Site(1234uL)), contact)

        assertThat(params.payment.paymentMethod).isEqualTo(TransactionPaymentMethod.USE_CREDITS)
        assertThat(params.cart).isEqualTo(testShoppingCart(CartKey.Site(1234uL)))
        assertThat(params.domainDetails).isEqualTo(contact)
    }

    @Test
    fun `an error code names the contact field it belongs to`() {
        assertThat(DomainContactField.fromApiErrorCode("first_name")).isEqualTo(DomainContactField.FIRST_NAME)
        assertThat(DomainContactField.fromApiErrorCode("address_1")).isEqualTo(DomainContactField.ADDRESS_1)
        assertThat(DomainContactField.fromApiErrorCode("postal_code")).isEqualTo(DomainContactField.POSTAL_CODE)
        assertThat(DomainContactField.fromApiErrorCode("country_code")).isEqualTo(DomainContactField.COUNTRY_CODE)
    }

    @Test
    fun `every field is reachable from its own error code`() {
        DomainContactField.entries.forEach {
            assertThat(DomainContactField.fromApiErrorCode(it.apiErrorCode)).isEqualTo(it)
        }
    }

    @Test
    fun `an error code is matched whatever its case`() {
        assertThat(DomainContactField.fromApiErrorCode("FIRST_NAME")).isEqualTo(DomainContactField.FIRST_NAME)
    }

    @Test
    fun `a code that names no field on the form maps to nothing`() {
        assertThat(DomainContactField.fromApiErrorCode("fax")).isNull()
        assertThat(DomainContactField.fromApiErrorCode("insufficient_funds")).isNull()
        assertThat(DomainContactField.fromApiErrorCode(null)).isNull()
    }

    private fun wpError(code: String, message: String) = WpRequestResult.WpError<Any>(
        errorCode = WpErrorCode.CustomException(code),
        errorMessage = message,
        statusCode = 400.toUInt(),
        response = "",
        requestUrl = "",
        requestMethod = RequestMethod.POST,
    )
}
