package org.wordpress.android.ui.stats.refresh.lists

import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.UiModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.util.NetworkUtilsWrapper

class StatsListViewModelTest : BaseUnitTest() {
    @Mock lateinit var statsUseCase: BaseListUseCase
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    private lateinit var viewModel: StatsListViewModel
    private val data = MutableLiveData<List<UseCaseModel>>()
    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        whenever(statsUseCase.data).thenReturn(data)
        viewModel = StatsListViewModel(Dispatchers.Unconfined, statsUseCase, networkUtilsWrapper)
    }

    @Test
    fun `returns success ui model when all the inputs are successful`() {
        val uiModels = mutableListOf<UiModel<out List<StatsBlock>>>()
        viewModel.uiModel.observeForever { uiModel ->
            uiModel?.let { uiModels.add(uiModel) }
        }
        data.value = listOf(UseCaseModel(InsightsTypes.FOLLOWER_TOTALS, data = listOf(), state = SUCCESS))

        assertThat(uiModels).hasSize(1)
        val model = uiModels.last() as UiModel.Success
        assertThat(model.data).hasSize(1)
        assertThat(model.data[0].statsTypes).isEqualTo(InsightsTypes.FOLLOWER_TOTALS)
        assertThat(model.data[0].type).isEqualTo(StatsBlock.Type.SUCCESS)
        assertThat(model.data[0].data).isEmpty()
    }
}
