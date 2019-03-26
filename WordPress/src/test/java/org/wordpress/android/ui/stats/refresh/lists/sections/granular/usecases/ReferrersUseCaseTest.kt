package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Group
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Referrer
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val itemsToLoad = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)
private val limitMode = Top(itemsToLoad)

class ReferrersUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: ReferrersStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: ReferrersUseCase
    private val firstGroupViews = 50
    private val secondGroupViews = 30
    private val singleReferrer = Group("group1", "Group 1", "group1.jpg", "group1.com", firstGroupViews, listOf())
    private val referrer = Referrer("Referrer 1", 20, "referrer.jpg", "referrer.com")
    private val group = Group("group2", "Group 2", "group2.jpg", "group2.com", secondGroupViews, listOf(referrer))
    @Before
    fun setUp() {
        useCase = ReferrersUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                store,
                statsSiteProvider,
                selectedDateProvider,
                tracker,
                BLOCK
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
        whenever((selectedDateProvider.getSelectedDateState(statsGranularity))).thenReturn(
                SelectedDate(
                        0,
                        listOf(selectedDate)
                )
        )
    }

    @Test
    fun `maps referrers to UI model`() = test {
        val forced = false
        val model = ReferrersModel(10, 15, listOf(singleReferrer, group), false)
        whenever(store.getReferrers(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(store.fetchReferrers(site,
                statsGranularity, limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsTypes.REFERRERS)
        val expandableItem = result.data!!.assertNonExpandedList()

        expandableItem.onExpandClicked(true)

        val updatedResult = loadData(true, forced)

        updatedResult.data!!.assertExpandedList()
    }

    private fun List<BlockListItem>.assertNonExpandedList(): ExpandableItem {
        assertThat(this).hasSize(4)
        assertTitle(this[0])
        assertLabel(this[1])
        assertSingleItem(
                this[2],
                singleReferrer.name!!,
                singleReferrer.total,
                singleReferrer.icon
        )
        return assertExpandableItem(this[3], group.name!!, group.total!!, group.icon)
    }

    private fun List<BlockListItem>.assertExpandedList(): ExpandableItem {
        assertThat(this).hasSize(6)
        assertTitle(this[0])
        assertLabel(this[1])
        assertSingleItem(
                this[2],
                singleReferrer.name!!,
                singleReferrer.total,
                singleReferrer.icon
        )
        val expandableItem = assertExpandableItem(this[3], group.name!!, group.total!!, group.icon)
        assertSingleItem(this[4], referrer.name, referrer.views, referrer.icon)
        assertThat(this[5]).isEqualTo(Divider)
        return expandableItem
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val model = ReferrersModel(10, 15, listOf(singleReferrer), true)
        whenever(store.getReferrers(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(store.fetchReferrers(site,
                statsGranularity, limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsTypes.REFERRERS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertLabel(this[1])
            assertSingleItem(
                    this[2],
                    singleReferrer.name!!,
                    singleReferrer.total,
                    singleReferrer.icon
            )
            assertLink(this[3])
        }
    }

    @Test
    fun `maps empty referrers to UI model`() = test {
        val forced = false
        whenever(store.fetchReferrers(site,
                statsGranularity, limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(ReferrersModel(0, 0, listOf(), false))
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
        result.stateData!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
            assertThat(this[1]).isEqualTo(BlockListItem.Empty(R.string.stats_no_data_for_period))
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(store.fetchReferrers(site,
                statsGranularity, limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_referrers)
    }

    private fun assertLabel(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).leftLabel).isEqualTo(R.string.stats_referrer_label)
        assertThat(item.rightLabel).isEqualTo(R.string.stats_referrer_views_label)
    }

    private fun assertSingleItem(
        item: BlockListItem,
        key: String,
        views: Int?,
        icon: String?
    ) {
        assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (views != null) {
            assertThat(item.value).isEqualTo(views.toString())
        } else {
            assertThat(item.value).isNull()
        }
        assertThat(item.iconUrl).isEqualTo(icon)
    }

    private fun assertExpandableItem(
        item: BlockListItem,
        label: String,
        views: Int,
        icon: String?
    ): ExpandableItem {
        assertThat(item.type).isEqualTo(EXPANDABLE_ITEM)
        assertThat((item as ExpandableItem).header.text).isEqualTo(label)
        assertThat(item.header.value).isEqualTo(views.toFormattedString())
        assertThat(item.header.iconUrl).isEqualTo(icon)
        return item
    }

    private fun assertLink(item: BlockListItem) {
        assertThat(item.type).isEqualTo(LINK)
        assertThat((item as Link).text).isEqualTo(R.string.stats_insights_view_more)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
