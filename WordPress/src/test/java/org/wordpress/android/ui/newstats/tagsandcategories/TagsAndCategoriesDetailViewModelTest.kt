package org.wordpress.android.ui.newstats.tagsandcategories

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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.TagData
import org.wordpress.android.ui.newstats.datasource.TagGroupData
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.ui.newstats.repository.TagsResult
import org.wordpress.android.viewmodel.ResourceProvider

/**
 * Tests specific to [TagsAndCategoriesDetailViewModel].
 * Base ViewModel behaviour (loading, errors, guards) is
 * covered by [BaseTagsAndCategoriesViewModelTest].
 */
@ExperimentalCoroutinesApi
class TagsAndCategoriesDetailViewModelTest :
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
    }

    private fun initViewModel() {
        viewModel = TagsAndCategoriesDetailViewModel(
            selectedSiteRepository,
            statsTagsUseCase,
            resourceProvider,
            mapper
        )
    }

    @Test
    fun `initial state is Loading`() {
        initViewModel()

        assertThat(viewModel.uiState.value)
            .isInstanceOf(
                TagsAndCategoriesCardUiState
                    .Loading::class.java
            )
    }

    @Test
    fun `when loadData, then fetches with detail max`() =
        test {
            whenever(
                statsTagsUseCase(any(), any(), any())
            ).thenReturn(createSuccessResult())

            initViewModel()
            viewModel.loadData()
            advanceUntilIdle()

            verify(statsTagsUseCase)
                .invoke(
                    eq(TEST_SITE_ID),
                    eq(DETAIL_MAX_ITEMS),
                    any()
                )
        }

    private fun createSuccessResult() =
        TagsResult.Success(
            StatsTagsData(
                tagGroups = listOf(
                    TagGroupData(
                        tags = listOf(
                            TagData(
                                tagType = "category",
                                name = "Uncategorized"
                            )
                        ),
                        views = 83L
                    )
                )
            )
        )

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val DETAIL_MAX_ITEMS = 100
    }
}
