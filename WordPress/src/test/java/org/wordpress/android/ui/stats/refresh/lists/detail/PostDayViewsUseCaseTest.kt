package org.wordpress.android.ui.stats.refresh.lists.detail

import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Day
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection.DETAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider

class PostDayViewsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: PostDetailStore
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var mapper: PostDayViewsMapper
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var statsPostProvider: StatsPostProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var title: ValueItem
    @Mock lateinit var barChartItem: BarChartItem
    @Mock lateinit var model: PostDetailStatsModel
    private lateinit var useCase: PostDayViewsUseCase
    private val postId = 1L
    @Before
    fun setUp() {
        whenever(selectedDateProvider.granularSelectedDateChanged(DETAIL)).thenReturn(MutableLiveData())
        useCase = PostDayViewsUseCase(
                Dispatchers.Unconfined,
                mapper,
                statsDateFormatter,
                selectedDateProvider,
                statsSiteProvider,
                statsPostProvider,
                store
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsPostProvider.postId).thenReturn(postId)
        whenever(mapper.buildTitle(any(), isNull())).thenReturn(title)
    }

    @Test
    fun `maps domain model to UI model`() = test {
        val forced = false
        whenever(mapper.buildChart(any(), any(), any(), any())).thenReturn(listOf(barChartItem))
        whenever(model.dayViews).thenReturn(listOf(Day("2019-10-10", 50)))

        whenever(store.getPostDetail(site, postId)).thenReturn(model)
        whenever(store.fetchPostDetail(site, postId, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(PostDetailTypes.POST_OVERVIEW)
        Assertions.assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            Assertions.assertThat(this[0]).isEqualTo(title)
            Assertions.assertThat(this[1]).isEqualTo(barChartItem)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchPostDetail(site, postId, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.state).isEqualTo(ERROR)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
