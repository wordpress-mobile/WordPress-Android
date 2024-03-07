package org.wordpress.android.ui.sitemonitor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class SiteMonitorTabViewModelSliceTest : BaseUnitTest() {
    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock
    private lateinit var accountStore: AccountStore
    @Mock
    private lateinit var mapper: SiteMonitorMapper
    @Mock
    private lateinit var siteMonitorUtils: SiteMonitorUtils
    @Mock
    private lateinit var siteStore: SiteStore

    private lateinit var viewModel: SiteMonitorTabViewModelSlice

    val site = mock<SiteModel>()

    val refreshStates = mutableListOf<Boolean>()

    @Before
    fun setUp() = test {
        viewModel = SiteMonitorTabViewModelSlice(
            testDispatcher(),
            networkUtilsWrapper,
            accountStore,
            mapper,
            siteMonitorUtils,
            siteStore
        )

        whenever(accountStore.account).thenReturn(mock())
        whenever(accountStore.account.userName).thenReturn(USER_NAME)
        whenever(accountStore.accessToken).thenReturn(ACCESS_TOKEN)

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(mapper.toGenericError(any())).thenReturn(mock())
        whenever(mapper.toNoNetworkError(any())).thenReturn(mock())
        whenever(mapper.toPrepared(any(), any(), any())).thenReturn(mock())

        whenever(site.url).thenReturn(URL)
        whenever(siteMonitorUtils.sanitizeSiteUrl(any())).thenReturn(URL)
        whenever(siteMonitorUtils.getAuthenticationPostData(any(), any(), any(), any(), any())).thenReturn(URL)

        viewModel.initialize(testScope())
    }

    @Test
    fun `when slice is instantiated, then uiState is in preparing`() {
        assertThat(viewModel.uiState.value).isEqualTo(SiteMonitorUiState.Preparing)
    }

    @Test
    fun `given loadView(), when slice is started, then uiState is in prepared`() = test {
        viewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.Prepared::class.java)
    }

    @Test
    fun `given null username, when slice is started, then uiState is in toGenericError`() {
        whenever(accountStore.account.userName).thenReturn(null)

        viewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.GenericError::class.java)
    }

    @Test
    fun `given null accessToken, when slice is started, then uiState is in toGenericError`() {
        whenever(accountStore.accessToken).thenReturn(null)

        viewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.GenericError::class.java)
    }

    @Test
    fun `given no network, when slice is started, then uiState is in error`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.NoNetworkError::class.java)
    }

    @Test
    fun `given prepared state, when url is loaded, then uiState loaded is posted`() = test {
        viewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)
        advanceUntilIdle()
        viewModel.onUrlLoaded()

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.Loaded::class.java)
    }

    @Test
    fun `given preparing state, when url is loaded, then uiState loaded is not posted`() = test {
        viewModel.onUrlLoaded()

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.Preparing::class.java)
    }

    @Test
    fun `when web view error, then error state is posted`() = test {
        viewModel.onWebViewError()

        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.GenericError::class.java)
    }

    @Test
    fun `given loaded  state, when refresh is invoked, then uiState loaded is posted`() = test {
        viewModel.start(SiteMonitorType.METRICS, SiteMonitorTabItem.Metrics.urlTemplate, site)
        advanceUntilIdle()
        viewModel.onUrlLoaded()
        viewModel.refreshData()

        assertThat(viewModel.isRefreshing.value).isTrue()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(SiteMonitorUiState.Prepared::class.java)
        assertThat(viewModel.isRefreshing.value).isFalse()
    }

    companion object {
        const val USER_NAME = "user_name"
        const val ACCESS_TOKEN = "access_token"
        const val URL = "test.wordpress.com"
    }
}
