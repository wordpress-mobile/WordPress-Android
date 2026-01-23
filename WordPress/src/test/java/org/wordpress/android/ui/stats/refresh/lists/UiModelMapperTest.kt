package org.wordpress.android.ui.stats.refresh.lists

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.ManagementType
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class UiModelMapperTest : BaseUnitTest() {
    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    private lateinit var mapper: UiModelMapper

    @Before
    fun setUp() {
        mapper = UiModelMapper(networkUtilsWrapper)
    }

    // TODO restore this test when removing the forced empty view in UiModelMapper.mapInsights
    @Test
    fun `mapInsights returns empty ui model`() {
        var error: Int? = null
        val uiModel = mapper.mapInsights(
            listOf(
                UseCaseModel(TOTAL_FOLLOWERS, data = listOf(), state = SUCCESS),
                UseCaseModel(ManagementType.CONTROL, data = listOf(), state = SUCCESS)
            )
        ) {
            error = it
        }

        val model = uiModel as UiModel.Empty
        assertThat(model.title).isEqualTo(R.string.stats_empty_insights_title)
        assertThat(model.subtitle).isEqualTo(R.string.stats_insights_management_title)
        assertThat(model.showButton).isTrue()
        assertThat(error).isNull()
    }

    @Test
    fun `mapInsights returns empty when there are only management blocks visible`() {
        var error: Int? = null
        val uiModel = mapper.mapInsights(
            listOf(
                UseCaseModel(ManagementType.NEWS_CARD, data = listOf(), state = SUCCESS),
                UseCaseModel(ManagementType.CONTROL, data = listOf(), state = SUCCESS)
            )
        ) {
            error = it
        }

        val model = uiModel as UiModel.Empty
        assertThat(model.title).isEqualTo(R.string.stats_empty_insights_title)
        assertThat(model.subtitle).isEqualTo(R.string.stats_insights_management_title)
        assertThat(model.showButton).isTrue()
        assertThat(error).isNull()
    }
}
