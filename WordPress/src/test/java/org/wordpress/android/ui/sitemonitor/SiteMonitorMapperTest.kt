package org.wordpress.android.ui.sitemonitor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteMonitorMapperTest : BaseUnitTest() {
    @Mock
    lateinit var siteMonitorUtils: SiteMonitorUtils

    private lateinit var siteMonitorMapper: SiteMonitorMapper

    @Before
    fun setup() {
        siteMonitorMapper = SiteMonitorMapper(siteMonitorUtils)
    }

    @Test
    fun `given prepared request, when mapper is called, then site monitor model is created`() {
        whenever(siteMonitorUtils.getUserAgent()).thenReturn(USER_AGENT)

        val state = siteMonitorMapper.toPrepared(URL, ADDRESS_TO_LOAD, SiteMonitorType.METRICS)

        assertThat(state.model.siteMonitorType).isEqualTo(SiteMonitorType.METRICS)
        assertThat(state.model.url).isEqualTo(URL)
        assertThat(state.model.addressToLoad).isEqualTo(ADDRESS_TO_LOAD)
        assertThat(state.model.userAgent).isEqualTo(USER_AGENT)
    }

    @Test
    fun `given network error, when mapper is called, then NoNetwork error is created`() {
        val state = siteMonitorMapper.toNoNetworkError(mock())

        assertThat(state).isInstanceOf(SiteMonitorUiState.NoNetworkError::class.java)
    }

    @Test
    fun `given generic error error, when mapper is called, then Generic error is created`() {
        val state = siteMonitorMapper.toGenericError(mock())

        assertThat(state).isInstanceOf(SiteMonitorUiState.GenericError::class.java)
    }

    companion object {
        const val USER_AGENT = "user_agent"
        const val URL = "url"
        const val ADDRESS_TO_LOAD = "address_to_load"
    }
}
