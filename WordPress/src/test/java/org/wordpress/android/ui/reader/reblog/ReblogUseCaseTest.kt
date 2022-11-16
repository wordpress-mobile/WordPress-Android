package org.wordpress.android.ui.reader.reblog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.reblog.ReblogState.MultipleSites
import org.wordpress.android.ui.reader.reblog.ReblogState.NoSite
import org.wordpress.android.ui.reader.reblog.ReblogState.SingleSite

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReblogUseCaseTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var siteStore: SiteStore

    private lateinit var reblogUseCase: ReblogUseCase

    @Before
    fun setUp() = test {
        reblogUseCase = ReblogUseCase(siteStore, TEST_DISPATCHER)
    }

    @Test
    fun `when user has no visible WPCOM site the no site flow is triggered`() = test {
        val post = ReaderPost()
        val visibleWPComSites = listOf<SiteModel>() // No sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isEqualTo(NoSite)
    }

    @Test
    fun `when user has only one visible WPCOM site the post editor is triggered`() = test {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isInstanceOf(SingleSite::class.java)

        val peState = state as? SingleSite
        Assertions.assertThat(peState?.site).isEqualTo(site)
        Assertions.assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has more than one visible WPCOM sites the site picker is triggered`() = test {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isInstanceOf(MultipleSites::class.java)

        val spState = state as? MultipleSites
        Assertions.assertThat(spState?.defaultSite).isEqualTo(site)
        Assertions.assertThat(spState?.post).isEqualTo(post)
    }

    @Test
    fun `when having more than one visible WPCOM sites and selecting site to reblog the post editor is triggered`() =
            test {
        val siteId = 1
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val afterButtonClickedState = reblogUseCase.onReblogButtonClicked(post) as MultipleSites
        val state = reblogUseCase.onReblogSiteSelected(siteId, afterButtonClickedState.post)

        Assertions.assertThat(state).isInstanceOf(SingleSite::class.java)

        val peState = state as? SingleSite
        Assertions.assertThat(peState?.site).isEqualTo(site)
        Assertions.assertThat(peState?.post).isEqualTo(post)
    }
}
