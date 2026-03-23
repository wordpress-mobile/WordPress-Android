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
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.TagData
import org.wordpress.android.ui.newstats.datasource.TagGroupData
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TagsAndCategoriesViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var statsTagsUseCase: StatsTagsUseCase

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private val mapper = TagsAndCategoriesMapper()

    private lateinit var viewModel:
        TagsAndCategoriesViewModel

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

    private fun initViewModel() {
        viewModel = TagsAndCategoriesViewModel(
            selectedSiteRepository,
            statsTagsUseCase,
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
        }

    @Test
    fun `when exception is thrown, then error state with localized message`() =
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

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Loaded::class.java
            )
        }

    @Test
    fun `when fetch succeeds, then items are mapped correctly`() =
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
            assertThat(state.items[1].name)
                .isEqualTo(TEST_TAG_NAME)
            assertThat(state.items[1].views)
                .isEqualTo(TEST_TAG_VIEWS)
        }

    @Test
    fun `when fetch succeeds, then maxViewsForBar is first item views`() =
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

    @Test
    fun `when multi-tag group, then name is joined with separator`() =
        test {
            val multiTagGroup = TagGroupData(
                tags = listOf(
                    TagData(
                        tagType = "tag",
                        name = "Alpha"
                    ),
                    TagData(
                        tagType = "category",
                        name = "Beta"
                    )
                ),
                views = 50
            )
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(
                TagsResult.Success(
                    StatsTagsData(
                        tagGroups = listOf(multiTagGroup)
                    )
                )
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.items[0].name)
                .isEqualTo("Alpha / Beta")
            assertThat(state.items[0].tags).hasSize(2)
        }

    @Test
    fun `when all tags are categories, then displayType is CATEGORY`() =
        test {
            val group = TagGroupData(
                tags = listOf(
                    TagData(
                        tagType = "category",
                        name = "Cat1"
                    ),
                    TagData(
                        tagType = "category",
                        name = "Cat2"
                    )
                ),
                views = 50
            )
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(
                TagsResult.Success(
                    StatsTagsData(
                        tagGroups = listOf(group)
                    )
                )
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.items[0].displayType)
                .isEqualTo(TagGroupDisplayType.CATEGORY)
        }

    @Test
    fun `when all tags are tags, then displayType is TAG`() =
        test {
            val group = TagGroupData(
                tags = listOf(
                    TagData(
                        tagType = "tag",
                        name = "Tag1"
                    )
                ),
                views = 50
            )
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(
                TagsResult.Success(
                    StatsTagsData(
                        tagGroups = listOf(group)
                    )
                )
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.items[0].displayType)
                .isEqualTo(TagGroupDisplayType.TAG)
        }

    @Test
    fun `when mixed tags, then displayType is MIXED`() =
        test {
            val group = TagGroupData(
                tags = listOf(
                    TagData(
                        tagType = "tag",
                        name = "Tag1"
                    ),
                    TagData(
                        tagType = "category",
                        name = "Cat1"
                    )
                ),
                views = 50
            )
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(
                TagsResult.Success(
                    StatsTagsData(
                        tagGroups = listOf(group)
                    )
                )
            )

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            val state = viewModel.uiState.value
                as TagsAndCategoriesCardUiState.Loaded
            assertThat(state.items[0].displayType)
                .isEqualTo(TagGroupDisplayType.MIXED)
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
    // endregion

    // region loadData guards
    @Test
    fun `when loadData called twice, then fetch only once`() =
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
                .invoke(eq(TEST_SITE_ID), any(), any())
        }
    // endregion

    // region refresh
    @Test
    fun `when refresh, then data is re-fetched`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()
            viewModel.refresh()
            advanceUntilIdle()

            verify(statsTagsUseCase, times(2))
                .invoke(eq(TEST_SITE_ID), any(), any())
        }

    @Test
    fun `when refresh after error, then loaded state`() =
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

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Loaded::class.java
                )
        }
    // endregion

    // region refresh sets loading
    @Test
    fun `when refresh with no site, then error`() =
        test {
            stubNoSiteError()
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            viewModel.refresh()

            assertThat(viewModel.uiState.value)
                .isInstanceOf(
                    TagsAndCategoriesCardUiState
                        .Error::class.java
                )
        }
    // endregion

    // region loadData retries after error
    @Test
    fun `when loadData after error, then data is re-fetched`() =
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
