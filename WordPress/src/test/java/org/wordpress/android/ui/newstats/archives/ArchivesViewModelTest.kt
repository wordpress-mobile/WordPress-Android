package org.wordpress.android.ui.newstats.archives

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.ARCHIVE_SECTION_KEY_TAXONOMIES
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCardUiState
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.repository.ArchiveSectionItemData
import org.wordpress.android.ui.newstats.repository.ArchivesResult
import org.wordpress.android.ui.newstats.repository.MostViewedChildData
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class ArchivesViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: ArchivesViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(resourceProvider.getString(R.string.stats_error_no_site)).thenReturn("No site selected")
        whenever(resourceProvider.getString(R.string.stats_error_api)).thenReturn("API error")
        whenever(resourceProvider.getString(R.string.stats_archives_section_search)).thenReturn("Search terms")
        whenever(resourceProvider.getString(R.string.stats_archives_section_categories)).thenReturn("Categories")
        whenever(resourceProvider.getString(R.string.stats_archives_section_taxonomies)).thenReturn("Taxonomies")
    }

    private fun initViewModel() {
        viewModel = ArchivesViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
    }

    // region Error states
    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(MostViewedCardUiState.Error::class.java)
    }

    @Test
    fun `when fetch fails, then the error message is surfaced`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any()))
            .thenReturn(ArchivesResult.Error(R.string.stats_error_api))

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Error
        assertThat(state.message).isEqualTo("API error")
    }
    // endregion

    // region Success states
    @Test
    fun `when data loads, then groups become rows labelled from their key`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(successResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).hasSize(2)
        assertThat(state.items[0].title).isEqualTo("Search terms")
        assertThat(state.items[0].views).isEqualTo(SEARCH_VIEWS)
        assertThat(state.items[1].title).isEqualTo("Categories")
        assertThat(state.items[1].views).isEqualTo(CATEGORY_VIEWS)
        assertThat(state.maxViewsForBar).isEqualTo(SEARCH_VIEWS)
    }

    @Test
    fun `when data loads, then a group's entries become its children`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(successResult())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        val children = state.items[0].children
        assertThat(children).hasSize(2)
        assertThat(children[0].name).isEqualTo("kotlin")
        assertThat(children[0].url).isEqualTo("https://example.com/?s=kotlin")
        assertThat(children[0].views).isEqualTo(20L)
    }

    @Test
    fun `when a group has more than ten entries, then the card lists only ten`() = test {
        val entries = (1..25).map {
            MostViewedChildData(name = "term $it", url = null, views = (100 - it).toLong())
        }
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(
            archivesSuccess(
                ArchiveSectionItemData(key = "search", views = 500L, previousViews = 0L, entries = entries)
            )
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].children).hasSize(10)
        assertThat(state.items[0].children.last().name).isEqualTo("term 10")
        // The group's own total still counts every entry, not only the ten listed under it.
        assertThat(state.items[0].views).isEqualTo(500L)
    }

    @Test
    fun `when the site has custom taxonomies, then one Taxonomies row holds a level per taxonomy`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(
            archivesSuccess(
                ArchiveSectionItemData(
                    key = ARCHIVE_SECTION_KEY_TAXONOMIES,
                    views = 12L,
                    previousViews = 0L,
                    entries = listOf(
                        MostViewedChildData(
                            name = "ingredient",
                            url = null,
                            views = 8L,
                            children = listOf(
                                MostViewedChildData(name = "saffron", url = null, views = 8L)
                            )
                        )
                    )
                )
            )
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).hasSize(1)
        assertThat(state.items[0].title).isEqualTo("Taxonomies")
        val taxonomy = state.items[0].children.single()
        assertThat(taxonomy.name).isEqualTo("ingredient")
        assertThat(taxonomy.children.single().name).isEqualTo("saffron")
    }

    @Test
    fun `when a taxonomy has more than ten terms, then the card lists only ten of them`() = test {
        val terms = (1..25).map {
            MostViewedChildData(name = "term $it", url = null, views = (100 - it).toLong())
        }
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(
            archivesSuccess(
                ArchiveSectionItemData(
                    key = ARCHIVE_SECTION_KEY_TAXONOMIES,
                    views = 500L,
                    previousViews = 0L,
                    entries = listOf(
                        MostViewedChildData(name = "ingredient", url = null, views = 500L, children = terms)
                    )
                )
            )
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        val taxonomy = state.items[0].children.single()
        assertThat(taxonomy.children).hasSize(10)
        assertThat(taxonomy.children.last().name).isEqualTo("term 10")
    }

    @Test
    fun `when a group's key has no translation, then it is spelled out rather than shown raw`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(
            archivesSuccess(
                ArchiveSectionItemData(key = "page_condition", views = 5L, previousViews = 0L)
            )
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].title).isEqualTo("Page condition")
    }

    @Test
    fun `when a group grew, then its change is positive`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(
            archivesSuccess(
                ArchiveSectionItemData(key = "search", views = 150L, previousViews = 100L)
            )
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        val change = state.items[0].change as MostViewedChange.Positive
        assertThat(change.value).isEqualTo(50L)
        assertThat(change.percentage).isEqualTo(50.0)
    }

    @Test
    fun `when there are no groups, then the loaded state is empty`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(archivesSuccess())

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).isEmpty()
        assertThat(state.maxViewsForBar).isEqualTo(0L)
    }
    // endregion

    // region Fetching
    @Test
    fun `when loading the card, then the bounded card fetch is used, not the detail one`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(successResult())

        initViewModel()
        advanceUntilIdle()

        verify(statsRepository).fetchArchives(eq(TEST_SITE_ID), eq(StatsPeriod.Last7Days), any())
        verify(statsRepository, never()).fetchArchivesDetail(any(), any(), any())
    }

    @Test
    fun `when period changes, then data is reloaded`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(successResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchArchives(any(), any(), any())
        verify(statsRepository).fetchArchives(eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days), any())
    }

    @Test
    fun `when refresh is called, then data is fetched again`() = test {
        whenever(statsRepository.fetchArchives(any(), any(), any())).thenReturn(successResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        verify(statsRepository, times(2)).fetchArchives(eq(TEST_SITE_ID), any(), any())
        assertThat(viewModel.isRefreshing.value).isFalse()
    }
    // endregion

    private fun successResult() = archivesSuccess(
        ArchiveSectionItemData(
            key = "search",
            views = SEARCH_VIEWS,
            previousViews = 10L,
            entries = listOf(
                MostViewedChildData(name = "kotlin", url = "https://example.com/?s=kotlin", views = 20L),
                MostViewedChildData(name = "coroutines", url = null, views = 10L)
            )
        ),
        ArchiveSectionItemData(
            key = "cat",
            views = CATEGORY_VIEWS,
            previousViews = 5L,
            entries = listOf(MostViewedChildData(name = "benefits", url = null, views = 10L))
        )
    )

    private fun archivesSuccess(vararg sections: ArchiveSectionItemData) = ArchivesResult.Success(
        sections = sections.toList(),
        totalViews = sections.sumOf { it.views },
        totalViewsChange = sections.sumOf { it.viewsChange },
        totalViewsChangePercent = 0.0
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"
        private const val SEARCH_VIEWS = 30L
        private const val CATEGORY_VIEWS = 10L
    }
}
