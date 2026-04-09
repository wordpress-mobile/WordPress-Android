package org.wordpress.android.ui.newstats.utm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmItemData
import org.wordpress.android.ui.newstats.repository.UtmPostItemData
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
class UtmViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var viewModel: UtmViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        whenever(
            appPrefsWrapper.getStatsUtmCategory(TEST_SITE_ID)
        ).thenReturn(null)
    }

    private fun initViewModel() {
        viewModel = UtmViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            appPrefsWrapper
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
    }

    // region formatUtmName

    @Test
    fun `formats array name with two values`() = test {
        val result = createSuccessResult(
            listOf(createItem("""["impact","affiliate"]"""))
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
            as UtmCardUiState.Loaded
        assertThat(state.items.first().title)
            .isEqualTo("impact / affiliate")
    }

    @Test
    fun `formats array name with three values`() = test {
        val result = createSuccessResult(
            listOf(
                createItem(
                    """["campaign","source","medium"]"""
                )
            )
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
            as UtmCardUiState.Loaded
        assertThat(state.items.first().title)
            .isEqualTo("campaign / source / medium")
    }

    @Test
    fun `formats single value array name`() = test {
        val result = createSuccessResult(
            listOf(createItem("""["google"]"""))
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
            as UtmCardUiState.Loaded
        assertThat(state.items.first().title)
            .isEqualTo("google")
    }

    @Test
    fun `passes through plain string name`() = test {
        val result = createSuccessResult(
            listOf(createItem("google"))
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
            as UtmCardUiState.Loaded
        assertThat(state.items.first().title)
            .isEqualTo("google")
    }

    // endregion

    // region Error states

    @Test
    fun `shows error when site is null`() = test {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state)
            .isInstanceOf(UtmCardUiState.Error::class.java)
        assertThat(
            (state as UtmCardUiState.Error).messageResId
        ).isEqualTo(R.string.stats_error_no_site)
    }

    @Test
    fun `shows error when access token is empty`() = test {
        whenever(accountStore.accessToken).thenReturn("")

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state)
            .isInstanceOf(UtmCardUiState.Error::class.java)
        assertThat(
            (state as UtmCardUiState.Error).messageResId
        ).isEqualTo(R.string.stats_error_api)
    }

    // endregion

    // region Success states

    @Test
    fun `loaded state has items capped at 10`() = test {
        val items = (1..15).map {
            createItem("item_$it", views = (15 - it).toLong())
        }
        val result = createSuccessResult(items)
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
            as UtmCardUiState.Loaded
        assertThat(state.items).hasSize(10)
        assertThat(state.hasMoreItems).isTrue()
    }

    @Test
    fun `loaded state with top posts`() = test {
        val item = createItem(
            """["source","medium"]""",
            topPosts = listOf(
                UtmPostItemData("Post 1", 10L),
                UtmPostItemData("Post 2", 5L)
            )
        )
        val result = createSuccessResult(listOf(item))
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(result)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
            as UtmCardUiState.Loaded
        val uiItem = state.items.first()
        assertThat(uiItem.topPosts).hasSize(2)
        assertThat(uiItem.topPosts[0].title)
            .isEqualTo("Post 1")
        assertThat(uiItem.topPosts[0].views)
            .isEqualTo(10L)
    }

    // endregion

    private fun createItem(
        name: String,
        views: Long = 5L,
        topPosts: List<UtmPostItemData> = emptyList()
    ) = UtmItemData(
        name = name,
        views = views,
        topPosts = topPosts
    )

    private fun createSuccessResult(
        items: List<UtmItemData>
    ) = UtmResult.Success(
        items = items,
        totalViews = items.sumOf { it.views }
    )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_token"
    }
}
