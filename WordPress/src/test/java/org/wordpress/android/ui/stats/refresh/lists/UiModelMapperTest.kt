package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TOTALS
import org.wordpress.android.ui.stats.refresh.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Success
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event

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
        val navigationTarget = MutableLiveData<Event<NavigationTarget>>()
        val uiModel = mapper.mapInsights(
                listOf(
                        UseCaseModel(FOLLOWER_TOTALS, data = listOf(), state = SUCCESS))
        ) {
            error = it
        }

        val model = uiModel as UiModel.Success
        assertThat(model.data).hasSize(1)
        assertThat((model.data[0] as Success).statsType).isEqualTo(FOLLOWER_TOTALS)
        assertThat(model.data[0].type).isEqualTo(StatsBlock.Type.SUCCESS)
        assertThat(model.data[0].data).isEmpty()
        assertThat(error).isNull()
    }
}
