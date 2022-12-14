package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
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
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK_DETAIL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.TextStyle
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.PieChartItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.PIE_CHART
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ReferrerPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

private const val ITEMS_TO_LOAD = 300
private const val GROUP_ID_WORDPRESS = "WordPress.com Reader"
private const val GROUP_ID_SEARCH = "Search Engines"
private val statsGranularity = DAYS
private val selectedDate = Date(0)
private val limitMode = Top(ITEMS_TO_LOAD)

@ExperimentalCoroutinesApi
class ReferrersUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: ReferrersStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var popupMenuHandler: ReferrerPopupMenuHandler
    private lateinit var useCase: ReferrersUseCase
    private val firstGroupViews = 50
    private val secondGroupViews = 50
    private val thirdGroupViews = 40
    private val totalViews = firstGroupViews + secondGroupViews + thirdGroupViews
    private val wordPressReferrer = Group(
            GROUP_ID_WORDPRESS,
            "Group 1",
            "group1.jpg",
            "group1.com",
            firstGroupViews,
            listOf(),
            false
    )
    private val searchReferrer = Group(
            GROUP_ID_SEARCH,
            "Group 2",
            "group2.jpg",
            "group2.com",
            secondGroupViews,
            listOf(),
            false
    )
    private val referrer1 = Referrer("Referrer 1", 20, "referrer.jpg", "referrer.com", false)
    private val referrer2 = Referrer("Referrer 1", 20, "referrer.jpg", "referrer.com", true)
    private val group = Group(
            "group3",
            "Group 3",
            "group3.jpg",
            "group3.com",
            secondGroupViews,
            listOf(referrer1, referrer2),
            true
    )
    private val wordPressLegend = "Wordpress"
    private val searchLegend = "Search"
    private val othersLegend = "Others"
    private val totalLabel = "total"
    private val contentDescription = "title, views"

    @Before
    fun setUp() {
        useCase = ReferrersUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                coroutinesTestRule.testDispatcher,
                store,
                statsSiteProvider,
                selectedDateProvider,
                tracker,
                contentDescriptionHelper,
                statsUtils,
                resourceProvider,
                BLOCK_DETAIL,
                popupMenuHandler
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
        whenever((selectedDateProvider.getSelectedDateState(statsGranularity))).thenReturn(
                SelectedDate(
                        selectedDate,
                        listOf(selectedDate)
                )
        )
        whenever(resourceProvider.getString(R.string.stats_referrers_pie_chart_wordpress)).thenReturn(wordPressLegend)
        whenever(resourceProvider.getString(R.string.stats_referrers_pie_chart_search)).thenReturn(searchLegend)
        whenever(resourceProvider.getString(R.string.stats_referrers_pie_chart_others)).thenReturn(othersLegend)
        whenever(resourceProvider.getString(R.string.stats_referrers_pie_chart_total_label)).thenReturn(totalLabel)
        whenever(contentDescriptionHelper.buildContentDescription(any(), any())).thenReturn(contentDescription)
        whenever(
                contentDescriptionHelper.buildContentDescription(
                        any(),
                        any<String>(),
                        any()
                )
        ).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Ignore @Test
    fun `maps referrers to UI model`() = test {
        val forced = false
        val model = ReferrersModel(10, totalViews, listOf(wordPressReferrer, group, searchReferrer), false)
        whenever(store.getReferrers(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(store.fetchReferrers(site,
                statsGranularity, limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.REFERRERS)
        val expandableItem = result.data!!.assertNonExpandedList()

        expandableItem.onExpandClicked(true)

        val updatedResult = loadData(true, forced)

        updatedResult.data!!.assertExpandedList()
    }

    private fun List<BlockListItem>.assertNonExpandedList(): ExpandableItem {
        assertThat(this).hasSize(6)
        assertTitle(this[0])
        assertPieChartItem(this[1])
        assertLabel(this[2])
        assertSingleItem(
                this[3],
                wordPressReferrer.name!!,
                wordPressReferrer.total,
                wordPressReferrer.icon,
                wordPressReferrer.markedAsSpam
        )
        return assertExpandableItem(this[4], group.name!!, group.icon, group.markedAsSpam)
    }

    private fun List<BlockListItem>.assertExpandedList(): ExpandableItem {
        assertThat(this).hasSize(9)
        assertTitle(this[0])
        assertPieChartItem(this[1])
        assertLabel(this[2])
        assertSingleItem(
                this[3],
                wordPressReferrer.name!!,
                wordPressReferrer.total,
                wordPressReferrer.icon,
                wordPressReferrer.markedAsSpam
        )
        val expandableItem = assertExpandableItem(this[4], group.name!!, group.icon, group.markedAsSpam)
        assertSingleItem(this[5], referrer1.name, referrer1.views, referrer1.icon, referrer1.markedAsSpam)
        assertSingleItem(this[6], referrer2.name, referrer2.views, referrer2.icon, referrer2.markedAsSpam)
        assertThat(this[7]).isEqualTo(Divider)
        return expandableItem
    }

    @Ignore @Test
    fun `adds view more button when hasMore`() = test {
        useCase = ReferrersUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                coroutinesTestRule.testDispatcher,
                store,
                statsSiteProvider,
                selectedDateProvider,
                tracker,
                contentDescriptionHelper,
                statsUtils,
                resourceProvider,
                BLOCK,
                popupMenuHandler
        )

        val forced = false
        val limit = Top(6)
        val model = ReferrersModel(10, totalViews, listOf(wordPressReferrer), true)
        whenever(store.getReferrers(site, statsGranularity, limit, selectedDate)).thenReturn(model)
        whenever(store.fetchReferrers(site, statsGranularity, limit, selectedDate, forced))
                .thenReturn(OnStatsFetched(model))
        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.REFERRERS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertLabel(this[1])
            assertSingleItem(
                    this[2],
                    wordPressReferrer.name!!,
                    wordPressReferrer.total,
                    wordPressReferrer.icon,
                    wordPressReferrer.markedAsSpam
            )
            assertLink(this[3])
        }
    }

    @Ignore @Test
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

    @Ignore @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(
                store.fetchReferrers(
                        site,
                        statsGranularity, limitMode, selectedDate, forced
                )
        ).thenReturn(
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

    private fun assertPieChartItem(item: BlockListItem) {
        assertThat(item.type).isEqualTo(PIE_CHART)
        assertThat((item as PieChartItem).entries.first().label).isEqualTo(wordPressLegend)
        assertThat((item).entries.first().value).isEqualTo(wordPressReferrer.total)
        assertThat((item).entries[1].label).isEqualTo(searchLegend)
        assertThat((item).entries[1].value).isEqualTo(group.total)
        assertThat((item).entries[2].label).isEqualTo(othersLegend)
        assertThat((item).entries[2].value).isEqualTo(totalViews - firstGroupViews - secondGroupViews)
        assertThat((item).totalLabel).isEqualTo(totalLabel)
    }

    private fun assertLabel(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).startLabel).isEqualTo(R.string.stats_referrer_label)
        assertThat(item.endLabel).isEqualTo(R.string.stats_referrer_views_label)
    }

    private fun assertSingleItem(
        item: BlockListItem,
        key: String,
        views: Int?,
        icon: String?,
        spam: Boolean?
    ) {
        assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (views != null) {
            assertThat(item.value).isEqualTo(views.toString())
        } else {
            assertThat(item.value).isNull()
        }
        if (spam != null && spam) {
            assertThat(item.icon).isEqualTo(R.drawable.ic_spam_white_24dp)
            assertThat(item.textStyle).isEqualTo(TextStyle.LIGHT)
            assertThat(item.iconUrl).isNull()
        } else {
            assertThat(item.icon).isNull()
            assertThat(item.textStyle).isEqualTo(TextStyle.NORMAL)
            assertThat(item.iconUrl).isEqualTo(icon)
        }
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    private fun assertExpandableItem(
        item: BlockListItem,
        label: String,
        icon: String?,
        spam: Boolean?
    ): ExpandableItem {
        assertThat(item.type).isEqualTo(EXPANDABLE_ITEM)
        assertThat((item as ExpandableItem).header.text).isEqualTo(label)

        if (spam != null && spam) {
            assertThat(item.header.icon).isEqualTo(R.drawable.ic_spam_white_24dp)
            assertThat(item.header.textStyle).isEqualTo(TextStyle.LIGHT)
            assertThat(item.header.iconUrl).isNull()
        } else {
            assertThat(item.header.icon).isNull()
            assertThat(item.header.textStyle).isEqualTo(TextStyle.NORMAL)
            assertThat(item.header.iconUrl).isEqualTo(icon)
        }
        assertThat(item.header.contentDescription).isEqualTo(contentDescription)
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
