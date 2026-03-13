package org.wordpress.android.ui.newstats.tagsandcategories

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.TagData
import org.wordpress.android.ui.newstats.datasource.TagGroupData
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TagsAndCategoriesDetailViewModelTest :
    BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private val mapper = TagsAndCategoriesMapper()

    private lateinit var viewModel:
        TagsAndCategoriesDetailViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(accountStore.accessToken)
            .thenReturn(TEST_ACCESS_TOKEN)
    }

    private fun stubNoSiteError() {
        whenever(
            resourceProvider.getString(
                R.string.stats_error_no_site
            )
        ).thenReturn(NO_SITE_ERROR)
    }

    private fun stubApiError() {
        whenever(
            resourceProvider.getString(
                R.string.stats_error_api
            )
        ).thenReturn(API_ERROR)
    }

    private fun initViewModel() {
        viewModel = TagsAndCategoriesDetailViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider,
            mapper
        )
    }

    // region Initial state
    @Test
    fun `initial state is Loading`() {
        initViewModel()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Loading::class.java
            )
    }
    // endregion

    // region Error states
    @Test
    fun `when no site selected, then error state`() =
        test {
            stubNoSiteError()
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Error::class.java
            )
            assertThat(
                (state as TagsAndCategoriesCardUiState
                    .Error).message
            ).isEqualTo(NO_SITE_ERROR)
        }

    @Test
    fun `when access token is null, then error state`() =
        test {
            stubApiError()
            whenever(accountStore.accessToken)
                .thenReturn(null)

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Error::class.java
            )
            assertThat(
                (state as TagsAndCategoriesCardUiState
                    .Error).message
            ).isEqualTo(API_ERROR)
        }

    @Test
    fun `when fetch returns error, then error state`() =
        test {
            stubApiError()
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(
                TagsResult.Error("Network error")
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Error::class.java
            )
        }

    @Test
    fun `when exception thrown, then error state`() =
        test {
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenThrow(
                RuntimeException("Test exception")
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Error::class.java
            )
            assertThat(
                (state as TagsAndCategoriesCardUiState
                    .Error).message
            ).isEqualTo("Test exception")
        }
    // endregion

    // region Success states
    @Test
    fun `when fetch succeeds, then loaded state`() =
        test {
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Loaded::class.java
            )
        }

    @Test
    fun `when fetch succeeds, then items mapped correctly`() =
        test {
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.items).hasSize(2)
            assertThat(state.items[0].name)
                .isEqualTo(TEST_CATEGORY_NAME)
            assertThat(state.items[0].views)
                .isEqualTo(TEST_CATEGORY_VIEWS)
        }

    @Test
    fun `when empty result, then loaded with empty list`() =
        test {
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(
                TagsResult.Success(
                    StatsTagsData(
                        tagGroups = emptyList()
                    )
                )
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.items).isEmpty()
        }
    // endregion

    // region loadData guard
    @Test
    fun `when loadData called twice, then fetch only once`() =
        test {
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchTags(any(), any())
        }

    @Test
    fun `when loadData after error, then retries`() =
        test {
            stubApiError()
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(
                TagsResult.Error("Network error")
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Error::class.java
                )

            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(createSuccessResult())

            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Loaded::class.java
                )
        }
    // endregion

    // region Repository interaction
    @Test
    fun `when loadData, then fetches with detail max`() =
        test {
            whenever(
                statsRepository.fetchTags(any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsRepository)
                .init(TEST_ACCESS_TOKEN)
            verify(statsRepository)
                .fetchTags(
                    eq(TEST_SITE_ID),
                    eq(DETAIL_MAX_ITEMS)
                )
        }
    // endregion

    private fun createSuccessResult() =
        TagsResult.Success(
            StatsTagsData(
                tagGroups = listOf(
                    TagGroupData(
                        tags = listOf(
                            TagData(
                                tagType = "category",
                                name = TEST_CATEGORY_NAME
                            )
                        ),
                        views = TEST_CATEGORY_VIEWS
                    ),
                    TagGroupData(
                        tags = listOf(
                            TagData(
                                tagType = "tag",
                                name = TEST_TAG_NAME
                            )
                        ),
                        views = TEST_TAG_VIEWS
                    )
                )
            )
        )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN =
            "test_access_token"
        private const val NO_SITE_ERROR =
            "No site selected"
        private const val API_ERROR =
            "Failed to load stats"
        private const val TEST_CATEGORY_NAME =
            "Uncategorized"
        private const val TEST_CATEGORY_VIEWS = 83L
        private const val TEST_TAG_NAME = "snaps"
        private const val TEST_TAG_VIEWS = 15L
        private const val DETAIL_MAX_ITEMS = 100
    }
}
