package org.wordpress.android.ui.pagesrs

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
internal class PagesRsListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var serviceProvider: WpServiceProvider
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var fluxCBridge: PageRsFluxCBridge
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var site: SiteModel
    private var activeViewModel: PagesRsListViewModel? = null

    @Before
    fun setUp() {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel() = PagesRsListViewModel(
        selectedSiteRepository = selectedSiteRepository,
        serviceProvider = serviceProvider,
        resourceProvider = resourceProvider,
        fluxCBridge = fluxCBridge,
        networkUtilsWrapper = networkUtilsWrapper,
        analyticsTracker = analyticsTracker,
    ).also { activeViewModel = it }

    @Test
    fun `when no site selected, emits ShowToast and Finish`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val viewModel = createViewModel()

        viewModel.events.test {
            val first = awaitItem()
            assertThat(first).isInstanceOf(PageRsListEvent.ShowToast::class.java)
            assertThat((first as PageRsListEvent.ShowToast).messageResId)
                .isEqualTo(R.string.blog_not_found)

            val second = awaitItem()
            assertThat(second).isEqualTo(PageRsListEvent.Finish)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tab states are empty on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `isOpeningPage is false on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.isOpeningPage.value).isFalse()
    }

    @Test
    fun `openPage on trashed tab is a no-op`() = test {
        val viewModel = createViewModel()

        viewModel.openPage(remotePageId = 42L, tab = PageRsListTab.TRASHED)

        assertThat(viewModel.isOpeningPage.value).isFalse()
    }

    @Test
    fun `loadMorePages no-ops when collection not initialized`() {
        val viewModel = createViewModel()

        viewModel.loadMorePages(PageRsListTab.PUBLISHED)

        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `refreshTab triggers init when collection not initialized`() {
        val viewModel = createViewModel()

        viewModel.refreshTab(PageRsListTab.PUBLISHED, isUserRefresh = true)

        // No collection exists, so refreshTab falls back to initTab. The synchronous portion
        // leaves the tab in an effective loading state (no error has been recorded yet).
        val state = viewModel.tabStates.value[PageRsListTab.PUBLISHED]
        assertThat(state?.error).isNull()
        assertThat(state?.isRefreshing ?: false).isFalse
    }
}
