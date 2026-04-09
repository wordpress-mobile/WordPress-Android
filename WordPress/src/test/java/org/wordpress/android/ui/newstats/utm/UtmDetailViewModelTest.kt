package org.wordpress.android.ui.newstats.utm

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmItemData
import org.wordpress.android.ui.newstats.repository.UtmPostItemData
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class UtmDetailViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var statsRepository: StatsRepository

    @Mock
    lateinit var resourceProvider: ResourceProvider

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
        adminUrl = "https://example.com/wp-admin"
    }

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
        whenever(
            resourceProvider.getString(
                R.string.stats_period_last_7_days
            )
        ).thenReturn("Last 7 days")
    }

    private fun createViewModel(
        categoryName: String =
            UtmCategory.SOURCE_MEDIUM.name,
        periodType: String = "last_7_days"
    ): UtmDetailViewModel {
        val handle = SavedStateHandle(
            mapOf(
                UtmDetailViewModel
                    .EXTRA_CATEGORY_NAME
                    to categoryName,
                UtmDetailViewModel
                    .EXTRA_PERIOD_TYPE
                    to periodType
            )
        )
        return UtmDetailViewModel(
            handle,
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
    }

    // region Error states

    @Test
    fun `shows error when site is null`() = test {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(null)

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(
            UtmDetailUiState.Error::class.java
        )
        assertThat(
            (state as UtmDetailUiState.Error)
                .messageResId
        ).isEqualTo(R.string.stats_error_no_site)
    }

    @Test
    fun `shows error when access token is empty`() =
        test {
            whenever(accountStore.accessToken)
                .thenReturn("")

            val vm = createViewModel()
            vm.loadData()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertThat(state).isInstanceOf(
                UtmDetailUiState.Error::class.java
            )
            assertThat(
                (state as UtmDetailUiState.Error)
                    .messageResId
            ).isEqualTo(R.string.stats_error_api)
        }

    @Test
    fun `shows error on repository error`() = test {
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(
            UtmResult.Error(
                R.string.stats_error_api,
                isAuthError = true
            )
        )

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        val state = vm.uiState.value
            as UtmDetailUiState.Error
        assertThat(state.messageResId)
            .isEqualTo(R.string.stats_error_api)
        assertThat(state.isAuthError).isTrue()
    }

    // endregion

    // region Success states

    @Test
    fun `loaded state maps all items`() = test {
        val items = (1..15).map {
            createItem(
                "item_$it",
                views = (15 - it).toLong()
            )
        }
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(createSuccessResult(items))

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        val state = vm.uiState.value
            as UtmDetailUiState.Loaded
        assertThat(state.items).hasSize(15)
    }

    @Test
    fun `loaded state includes top posts`() = test {
        val item = createItem(
            """["source","medium"]""",
            topPosts = listOf(
                UtmPostItemData("Post 1", 10L),
                UtmPostItemData("Post 2", 5L)
            )
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(createSuccessResult(listOf(item)))

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        val state = vm.uiState.value
            as UtmDetailUiState.Loaded
        val uiItem = state.items.first()
        assertThat(uiItem.topPosts).hasSize(2)
        assertThat(uiItem.topPosts[0].title)
            .isEqualTo("Post 1")
        assertThat(uiItem.topPosts[0].views)
            .isEqualTo(10L)
    }

    @Test
    fun `loaded state has correct totals`() = test {
        val items = listOf(
            createItem("a", views = 100L),
            createItem("b", views = 50L)
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(
            UtmResult.Success(
                items = items,
                totalViews = 150L
            )
        )

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        val state = vm.uiState.value
            as UtmDetailUiState.Loaded
        assertThat(state.totalViews).isEqualTo(150L)
        assertThat(state.maxViewsForBar)
            .isEqualTo(100L)
    }

    @Test
    fun `loaded state formats UTM names`() = test {
        val item = createItem(
            """["impact","affiliate"]"""
        )
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(createSuccessResult(listOf(item)))

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        val state = vm.uiState.value
            as UtmDetailUiState.Loaded
        assertThat(state.items.first().title)
            .isEqualTo("impact / affiliate")
    }

    // endregion

    // region Category and period resolution

    @Test
    fun `uses category from saved state`() = test {
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(
            createSuccessResult(
                listOf(createItem("test"))
            )
        )

        val vm = createViewModel(
            categoryName = UtmCategory.CAMPAIGN.name
        )
        vm.loadData()
        advanceUntilIdle()

        verify(statsRepository).fetchUtm(
            eq(TEST_SITE_ID),
            eq(UtmCategory.CAMPAIGN.keys),
            any()
        )
    }

    @Test
    fun `defaults to SOURCE_MEDIUM for invalid category`() =
        test {
            whenever(
                statsRepository.fetchUtm(
                    eq(TEST_SITE_ID), any(), any()
                )
            ).thenReturn(
                createSuccessResult(
                    listOf(createItem("test"))
                )
            )

            val vm = createViewModel(
                categoryName = "INVALID"
            )
            vm.loadData()
            advanceUntilIdle()

            verify(statsRepository).fetchUtm(
                eq(TEST_SITE_ID),
                eq(UtmCategory.SOURCE_MEDIUM.keys),
                any()
            )
        }

    // endregion

    // region Retry and dedup

    @Test
    fun `loadData only fetches once`() = test {
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(
            createSuccessResult(
                listOf(createItem("test"))
            )
        )

        val vm = createViewModel()
        vm.loadData()
        vm.loadData()
        advanceUntilIdle()

        verify(statsRepository).fetchUtm(
            any(), any(), any()
        )
    }

    @Test
    fun `retry re-fetches after failure`() = test {
        whenever(
            statsRepository.fetchUtm(
                eq(TEST_SITE_ID), any(), any()
            )
        ).thenReturn(
            UtmResult.Error(R.string.stats_error_api)
        ).thenReturn(
            createSuccessResult(
                listOf(createItem("test"))
            )
        )

        val vm = createViewModel()
        vm.loadData()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(
            UtmDetailUiState.Error::class.java
        )

        vm.retry()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(
            UtmDetailUiState.Loaded::class.java
        )
    }

    // endregion

    // region getAdminUrl

    @Test
    fun `getAdminUrl returns site admin URL`() {
        val vm = createViewModel()
        assertThat(vm.getAdminUrl())
            .isEqualTo("https://example.com/wp-admin")
    }

    @Test
    fun `getAdminUrl returns null when no site`() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(null)

        val vm = createViewModel()
        assertThat(vm.getAdminUrl()).isNull()
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
        private const val TEST_ACCESS_TOKEN =
            "test_token"
    }
}
