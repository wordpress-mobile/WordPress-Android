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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.domains.usecases.DesignatePrimaryDomainResult
import org.wordpress.android.ui.domains.usecases.DesignatePrimaryDomainUseCase
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SetPrimaryDomainResponse
import uniffi.wp_api.WpErrorCode

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class DesignatePrimaryDomainUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var wpComApiClientProvider: WpComApiClientProvider

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var wpComApiClient: WpComApiClient

    private lateinit var useCase: DesignatePrimaryDomainUseCase

    private val site = SiteModel().apply { siteId = 1234L }

    @Before
    fun setUp() {
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(wpComApiClientProvider.getWpComApiClient("test-token"))
            .thenReturn(wpComApiClient)
        useCase = DesignatePrimaryDomainUseCase(wpComApiClientProvider, accountStore)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given the domain is designated, when execute, returns success`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(WpRequestResult.Success(SetPrimaryDomainResponse(success = true)) as WpRequestResult<Any>)

        val result = useCase.execute(site, DOMAIN_NAME)

        assertThat(result).isEqualTo(DesignatePrimaryDomainResult.Success)
    }

    @Test
    fun `given the request fails, when execute, returns error carrying no message`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.UnknownError<Any>(
                    500.toUInt(),
                    "Internal Server Error",
                    "",
                    RequestMethod.POST
                )
            )

        val result = useCase.execute(site, DOMAIN_NAME)

        assertThat(result).isEqualTo(DesignatePrimaryDomainResult.Error())
    }

    @Test
    fun `given the API refuses the domain, when execute, the error carries its message`() = test {
        whenever(wpComApiClient.request<Any>(any()))
            .thenReturn(
                WpRequestResult.WpError<Any>(
                    errorCode = WpErrorCode.CustomException("paid_plan_required"),
                    errorMessage = "This site requires a paid plan",
                    statusCode = 400.toUInt(),
                    response = "",
                    requestUrl = "",
                    requestMethod = RequestMethod.POST,
                )
            )

        val result = useCase.execute(site, DOMAIN_NAME)

        assertThat(result).isEqualTo(DesignatePrimaryDomainResult.Error("This site requires a paid plan"))
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

        val result = useCase.execute(site, DOMAIN_NAME)

        assertThat(result).isEqualTo(DesignatePrimaryDomainResult.Error(isDeviceOffline = true))
    }

    @Test
    fun `given no access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        val result = useCase.execute(site, DOMAIN_NAME)

        assertThat(result).isEqualTo(DesignatePrimaryDomainResult.Error())
    }

    @Test
    fun `given a blank access token, when execute, returns error`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        val result = useCase.execute(site, DOMAIN_NAME)

        assertThat(result).isEqualTo(DesignatePrimaryDomainResult.Error())
    }

    companion object {
        private const val DOMAIN_NAME = "domainname.com"
    }
}
