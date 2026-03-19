package org.wordpress.android.ui.newstats.tagsandcategories

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
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.TagData
import org.wordpress.android.ui.newstats.datasource.TagGroupData
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.viewmodel.ResourceProvider

/**
 * Tests for [BaseTagsAndCategoriesViewModel] using
 * a concrete test subclass.
 */
@ExperimentalCoroutinesApi
class BaseTagsAndCategoriesViewModelTest :
    BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var statsTagsUseCase:
        StatsTagsUseCase

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private val mapper = TagsAndCategoriesMapper()

    private lateinit var viewModel:
        TestTagsAndCategoriesViewModel

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
    }

    private fun initViewModel(
        maxItems: Int = TEST_MAX_ITEMS
    ) {
        viewModel = TestTagsAndCategoriesViewModel(
            selectedSiteRepository,
            statsTagsUseCase,
            resourceProvider,
            mapper,
            maxItems
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

    // region loadData guard

    @Test
    fun `when loadData called twice, then fetch once`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsTagsUseCase, times(1))
                .invoke(any(), any(), any())
        }

    @Test
    fun `when loadData after success, then no refetch`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsTagsUseCase, times(1))
                .invoke(any(), any(), any())
        }

    @Test
    fun `when loadData after error, then retries`() =
        test {
            stubApiError()
            whenever(
                statsTagsUseCase(any(), any(), any())
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
                statsTagsUseCase(any(), any(), any())
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

    // region Error states

    @Test
    fun `when no site, then error state`() =
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
    fun `when no site, then use case not called`() =
        test {
            stubNoSiteError()
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsTagsUseCase, never())
                .invoke(any(), any(), any())
        }

    @Test
    fun `when fetch returns error, then error state`() =
        test {
            stubApiError()
            whenever(
                statsTagsUseCase(any(), any(), any())
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
            assertThat(
                (state as TagsAndCategoriesCardUiState
                    .Error).message
            ).isEqualTo(API_ERROR)
        }

    @Test
    fun `when exception thrown, then error state`() =
        test {
            stubUnknownError()
            whenever(
                statsTagsUseCase(any(), any(), any())
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
            ).isEqualTo(UNKNOWN_ERROR)
        }

    // endregion

    // region Success states

    @Test
    fun `when fetch succeeds, then loaded state`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Loaded::class.java
                )
        }

    @Test
    fun `when fetch succeeds, then items mapped`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
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
    fun `when empty result, then NoData state`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
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

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .NoData::class.java
                )
        }

    @Test
    fun `maxViewsForBar equals first item views`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.maxViewsForBar)
                .isEqualTo(TEST_CATEGORY_VIEWS)
        }

    // endregion

    // region maxItems passed to use case

    @Test
    fun `maxItems is passed to use case`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel(maxItems = 42)
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsTagsUseCase).invoke(
                eq(TEST_SITE_ID), eq(42), any()
            )
        }

    // endregion

    // region resetForRefresh

    @Test
    fun `resetForRefresh sets loading state`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Loaded::class.java
                )

            viewModel.callResetForRefresh()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Loading::class.java
                )
        }

    @Test
    fun `resetForRefresh allows refetch`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            viewModel.callResetForRefresh()
            viewModel.callFetchData(forceRefresh = true)
            advanceUntilIdle()

            verify(statsTagsUseCase, times(2))
                .invoke(any(), any(), any())
        }

    // endregion

    // region Helpers

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

    private fun stubUnknownError() {
        whenever(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn(UNKNOWN_ERROR)
    }

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

    // endregion

    /**
     * Concrete subclass exposing protected methods
     * for testing.
     */
    private class TestTagsAndCategoriesViewModel(
        selectedSiteRepository: SelectedSiteRepository,
        statsTagsUseCase: StatsTagsUseCase,
        resourceProvider: ResourceProvider,
        mapper: TagsAndCategoriesMapper,
        override val maxItems: Int
    ) : BaseTagsAndCategoriesViewModel(
        selectedSiteRepository,
        statsTagsUseCase,
        resourceProvider,
        mapper
    ) {
        fun callResetForRefresh() = resetForRefresh()
        fun callFetchData(
            forceRefresh: Boolean = false
        ) = fetchData(forceRefresh)
    }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_MAX_ITEMS = 10
        private const val NO_SITE_ERROR =
            "No site selected"
        private const val API_ERROR =
            "Failed to load stats"
        private const val UNKNOWN_ERROR =
            "An unknown error occurred"
        private const val TEST_CATEGORY_NAME =
            "Uncategorized"
        private const val TEST_CATEGORY_VIEWS = 83L
        private const val TEST_TAG_NAME = "snaps"
        private const val TEST_TAG_VIEWS = 15L
    }
}
