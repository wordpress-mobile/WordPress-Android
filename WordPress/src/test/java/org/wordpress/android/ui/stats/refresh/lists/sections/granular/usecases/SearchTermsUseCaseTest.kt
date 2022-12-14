package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel.SearchTerm
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType
import org.wordpress.android.fluxc.store.stats.time.SearchTermsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val ITEMS_TO_LOAD = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)

@ExperimentalCoroutinesApi
class SearchTermsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: SearchTermsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    @Mock lateinit var statsUtils: StatsUtils
    private lateinit var useCase: SearchTermsUseCase
    private val searchTerm = SearchTerm("search term", 10)
    private val contentDescription = "title, views"

    private val limitMode = Top(ITEMS_TO_LOAD)

    @Before
    fun setUp() {
        useCase = SearchTermsUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                coroutinesTestRule.testDispatcher,
                store,
                statsSiteProvider,
                selectedDateProvider,
                tracker,
                contentDescriptionHelper,
                statsUtils,
                BLOCK
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
        whenever((selectedDateProvider.getSelectedDateState(statsGranularity))).thenReturn(
                SelectedDate(
                        selectedDate,
                        listOf(selectedDate)
                )
        )
        whenever(contentDescriptionHelper.buildContentDescription(
                any(),
                any<Int>(),
                any()
        )).thenReturn(contentDescription)
        whenever(contentDescriptionHelper.buildContentDescription(
                any(),
                any<String>(),
                any()
        )).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `maps search_terms to UI model`() = test {
        val forced = false
        val model = SearchTermsModel(10, 15, 0, listOf(searchTerm), false)
        whenever(store.getSearchTerms(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(
                store.fetchSearchTerms(
                        site, statsGranularity, limitMode, selectedDate,
                        forced
                )
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.SEARCH_TERMS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertTitle(this[0])
            assertHeader(this[1])
            assertItem(this[2], searchTerm.text, searchTerm.views)
        }
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val model = SearchTermsModel(10, 15, 0, listOf(searchTerm), true)
        whenever(store.getSearchTerms(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(
                store.fetchSearchTerms(site, statsGranularity, limitMode, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.SEARCH_TERMS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertLink(this[3])
        }
    }

    @Test
    fun `adds unknown item when there are encrypted terms`() = test {
        val forced = false
        val unknownSearchCount = 500
        val model = SearchTermsModel(
                0,
                0,
                unknownSearchCount,
                listOf(searchTerm, searchTerm, searchTerm, searchTerm, searchTerm, searchTerm),
                false
        )
        whenever(store.getSearchTerms(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(
                store.fetchSearchTerms(site, statsGranularity, limitMode, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.SEARCH_TERMS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(8)
            assertTitle(this[0])
            assertHeader(this[1])
            assertItem(this[2], searchTerm.text, searchTerm.views)
            assertItem(this[3], searchTerm.text, searchTerm.views)
            assertItem(this[4], searchTerm.text, searchTerm.views)
            assertItem(this[5], searchTerm.text, searchTerm.views)
            assertItem(this[6], searchTerm.text, searchTerm.views)
            val unknownItem = this[7] as ListItemWithIcon
            assertThat(unknownItem.textResource).isEqualTo(R.string.stats_search_terms_unknown_search_terms)
            assertThat(unknownItem.value).isEqualTo(unknownSearchCount.toString())
        }
    }

    @Test
    fun `maps empty search_terms to UI model`() = test {
        val forced = false
        val model = SearchTermsModel(0, 0, 0, listOf(), false)
        whenever(store.getSearchTerms(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(
                store.fetchSearchTerms(site, statsGranularity, limitMode, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(model)
        )

        val result = loadData(true, forced)

        assertThat(result.type).isEqualTo(TimeStatsType.SEARCH_TERMS)
        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
        result.stateData!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
            assertThat(this[1]).isEqualTo(Empty(R.string.stats_no_data_for_period))
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(
                store.fetchSearchTerms(site, statsGranularity, limitMode, selectedDate, forced)
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
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_search_terms)
    }

    private fun assertHeader(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).startLabel).isEqualTo(R.string.stats_search_terms_label)
        assertThat(item.endLabel).isEqualTo(R.string.stats_search_terms_views_label)
    }

    private fun assertItem(
        item: BlockListItem,
        key: String,
        views: Int?
    ) {
        assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (views != null) {
            assertThat(item.value).isEqualTo(views.toString())
        } else {
            assertThat(item.value).isNull()
        }
        assertThat(item.contentDescription).isEqualTo(contentDescription)
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
