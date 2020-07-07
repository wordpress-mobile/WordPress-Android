package org.wordpress.android.ui.reader.reblog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost

@RunWith(MockitoJUnitRunner::class)
class ReblogUseCaseTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var siteStore: SiteStore

    private lateinit var reblogUseCase: ReblogUseCase

    @Before
    fun setUp() {
        reblogUseCase = ReblogUseCase(siteStore)
    }

    @Test
    fun `when user has no visible WPCOM site the no site flow is triggered`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf<SiteModel>() // No sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isEqualTo(NoSite)
    }

    @Test
    fun `when user has only one visible WPCOM site the post editor is triggered`() {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isInstanceOf(PostEditor::class.java)

        val peState = state as? PostEditor
        Assertions.assertThat(peState?.site).isEqualTo(site)
        Assertions.assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has more than one visible WPCOM sites the site picker is triggered`() {
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isInstanceOf(SitePicker::class.java)

        val spState = state as? SitePicker
        Assertions.assertThat(spState?.site).isEqualTo(site)
        Assertions.assertThat(spState?.post).isEqualTo(post)
    }

    @Test
    fun `when having more than one visible WPCOM sites and selecting site to reblog the post editor is triggered`() {
        val siteId = 1
        val site = SiteModel()
        val post = ReaderPost()
        val visibleWPComSites = listOf(site, site) // More sites

        whenever(siteStore.getSiteByLocalId(siteId)).thenReturn(site)
        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val afterButtonClickedState = reblogUseCase.onReblogButtonClicked(post) as SitePicker
        val state = reblogUseCase.onReblogSiteSelected(siteId, afterButtonClickedState.post)

        Assertions.assertThat(state).isInstanceOf(PostEditor::class.java)

        val peState = state as? PostEditor
        Assertions.assertThat(peState?.site).isEqualTo(site)
        Assertions.assertThat(peState?.post).isEqualTo(post)
    }

    @Test
    fun `when user has only one visible WPCOM site but the selected site is not retrieved an error occurs`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf(null) // One site

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isInstanceOf(Unknown::class.java)
    }

    @Test
    fun `when user has more than one visible WPCOM sites but the selected site is not retrieved an error occurs`() {
        val post = ReaderPost()
        val visibleWPComSites = listOf(null, null) // More sites

        whenever(siteStore.visibleSitesAccessedViaWPCom).thenReturn(visibleWPComSites)

        val state = reblogUseCase.onReblogButtonClicked(post)

        Assertions.assertThat(state).isInstanceOf(Unknown::class.java)
    }
}
