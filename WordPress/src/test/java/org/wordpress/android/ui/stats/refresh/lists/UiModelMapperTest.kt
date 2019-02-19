package org.wordpress.android.ui.stats.refresh.lists

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWER_TOTALS
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper

class UiModelMapperTest : BaseUnitTest() {
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    private lateinit var mapper: UiModelMapper
    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        mapper = UiModelMapper(networkUtilsWrapper)
    }

    @Test
    fun `mapInsights returns success ui model when all the inputs are successful`() {
        var error: Int? = null
        val uiModel = mapper.mapInsights(listOf(UseCaseModel(FOLLOWER_TOTALS, data = listOf(), state = SUCCESS))) {
            error = it
        }

        val model = uiModel as UiModel.Success
        assertThat(model.data).hasSize(1)
        assertThat(model.data[0].statsTypes).isEqualTo(InsightsTypes.FOLLOWER_TOTALS)
        assertThat(model.data[0].type).isEqualTo(StatsBlock.Type.SUCCESS)
        assertThat(model.data[0].data).isEmpty()
        assertThat(error).isNull()
    }
}
