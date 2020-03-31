package org.wordpress.android.ui.stats.refresh.lists.detail

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Day
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Week
import org.wordpress.android.fluxc.model.stats.PostDetailStatsModel.Year
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.PostDetailStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.EMPTY
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.BarChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ValueItem
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsPostProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.viewmodel.ResourceProvider

class PostDayViewsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: PostDetailStore
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var mapper: PostDayViewsMapper
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var statsPostProvider: StatsPostProvider
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var title: ValueItem
    @Mock lateinit var barChartItem: BarChartItem
    @Mock lateinit var emptyModel: PostDetailStatsModel
    @Mock lateinit var model: PostDetailStatsModel
    private lateinit var useCase: PostDayViewsUseCase
    private val postId = 1L
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = PostDayViewsUseCase(
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
                mapper,
                statsDateFormatter,
                selectedDateProvider,
                statsSiteProvider,
                statsPostProvider,
                store,
                resourceProvider
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsPostProvider.postId).thenReturn(postId)
        whenever(mapper.buildTitle(any(), isNull(), any())).thenReturn(title)
        whenever(resourceProvider.getString(R.string.stats_loading_card)).thenReturn("Loading")
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

        assertThat(result.type).isEqualTo(PostDetailType.POST_OVERVIEW)
        assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.apply {
            assertThat(this[0]).isEqualTo(title)
            assertThat(this[1]).isEqualTo(barChartItem)
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

        assertThat(result.state).isEqualTo(ERROR)
    }

    /**
     * Note that this test covers an edge condition tracked in GitHub issue
     * https://github.com/wordpress-mobile/WordPress-Android/issues/10830
     * For some context see
     * See https://github.com/wordpress-mobile/WordPress-Android/pull/10850#issuecomment-559555035
     */
    @Test
    fun `manage edge condition with data available but empty list`() = test {
        val forced = false

        whenever(emptyModel.dayViews).thenReturn(listOf())
        whenever(model.dayViews).thenReturn(listOf(Day("2019-10-10", 50)))
        whenever(store.getPostDetail(site, postId)).thenReturn(emptyModel)
        whenever(store.fetchPostDetail(site, postId, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(SUCCESS)
        assertThat(result.data).isEmpty()
    }

    @Test
    fun `maps list of empty items to empty UI model`() = test {
        val forced = false
        whenever(store.fetchPostDetail(site, postId, forced)).thenReturn(
                OnStatsFetched(
                        model = PostDetailStatsModel(
                                0,
                                listOf(Day("1970", 0), Day("1975", 0)),
                                listOf(Week(listOf(), 10, 10)),
                                listOf(Year(2020, listOf(), 100)),
                                listOf(Year(2020, listOf(), 100))
                        )
                )
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(EMPTY)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
