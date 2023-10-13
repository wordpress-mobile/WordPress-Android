package org.wordpress.android.ui.domains

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.network.rest.wpcom.site.AllDomainsDomain
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.domains.usecases.AllDomains
import org.wordpress.android.ui.domains.usecases.FetchAllDomainsUseCase

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchAllDomainsUseCaseTest: BaseUnitTest() {
    @Mock
    lateinit var store: SiteStore

    private lateinit var fetchAllDomainsUseCase: FetchAllDomainsUseCase

    @Before
    fun setUp() {
        fetchAllDomainsUseCase = FetchAllDomainsUseCase(store)
    }

    @Test
    fun `given all-domains call returns results, when the usecase is execute, returns success`() = runTest {
        whenever(store.fetchAllDomains()).thenReturn(
            SiteStore.FetchedAllDomainsPayload(
                listOf(
                    AllDomainsDomain("test.domain.one"),
                    AllDomainsDomain("test.domain.two")
                )
            )
        )

        val result = fetchAllDomainsUseCase.execute()

        assertThat(result is AllDomains.Success).isTrue
        with(result as AllDomains.Success) {
            assertThat(domains.size).isEqualTo(2)
            assertThat(domains[0].domain).isEqualTo("test.domain.one")
            assertThat(domains[1].domain).isEqualTo("test.domain.two")
        }
    }

    @Test
    fun `given the all-domain call returns error, when usecase is execute, returns error`() = runTest {
        whenever(store.fetchAllDomains()).thenReturn(
            SiteStore.FetchedAllDomainsPayload(
                SiteStore.AllDomainsError(
                    SiteStore.AllDomainsErrorType.GENERIC_ERROR,
                    null
                )
            )
        )

        val result = fetchAllDomainsUseCase.execute()

        assertThat(result is AllDomains.Error).isTrue
    }

    @Test
    fun `given the all-domain call returns empty response, when usecase execute, returns empty`() = runTest {
        whenever(store.fetchAllDomains()).thenReturn(
            SiteStore.FetchedAllDomainsPayload(listOf())
        )

        val result = fetchAllDomainsUseCase.execute()

        assertThat(result is AllDomains.Empty).isTrue
    }

    @Test
    fun `given the all-domain call returns null response, when usecase execute, returns empty`() = runTest {
        whenever(store.fetchAllDomains()).thenReturn(
            SiteStore.FetchedAllDomainsPayload(null)
        )

        val result = fetchAllDomainsUseCase.execute()

        assertThat(result is AllDomains.Empty).isTrue
    }
}
