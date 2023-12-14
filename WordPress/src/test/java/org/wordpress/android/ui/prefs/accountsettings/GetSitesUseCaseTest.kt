package org.wordpress.android.ui.prefs.accountsettings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.accountsettings.usecase.GetSitesUseCase

@ExperimentalCoroutinesApi
class GetSitesUseCaseTest: BaseUnitTest() {
    private lateinit var useCase: GetSitesUseCase

    @Mock
    private lateinit var siteStore: SiteStore

    val mockAtomicSite: SiteModel = mock()
    val mockNonAtomicSite: SiteModel = mock()

    @Before
    fun setUp() = test {
        useCase = GetSitesUseCase(
            testDispatcher(),
            siteStore,
        )
        whenever(mockAtomicSite.isWPComAtomic).thenReturn(true)
        whenever(mockNonAtomicSite.isWPComAtomic).thenReturn(false)
        whenever(siteStore.sitesAccessedViaWPComRest).thenReturn(listOf(
            mockAtomicSite,
            mockNonAtomicSite,
        ))
    }

    @Test
    fun `getAtomic filters for Atomic sites`() {
        test {
            val atomicSites = useCase.getAtomic()
            assertThat(atomicSites.size).isEqualTo(1)
            assertThat(atomicSites.first()).isEqualTo(mockAtomicSite)
        }
    }
}
