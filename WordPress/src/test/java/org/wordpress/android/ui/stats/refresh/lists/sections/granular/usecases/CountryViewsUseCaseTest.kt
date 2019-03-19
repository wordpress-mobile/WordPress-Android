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
import org.wordpress.android.fluxc.model.stats.time.CountryViewsModel
import org.wordpress.android.fluxc.model.stats.time.CountryViewsModel.Country
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.CountryViewsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.MapItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val itemsToLoad = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)

private val limitMode = Top(itemsToLoad)

class CountryViewsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: CountryViewsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: CountryViewsUseCase
    private val country = Country("CZ", "Czech Republic", 500, "flag.jpg", "flatFlag.jpg")
    @Before
    fun setUp() {
        useCase = CountryViewsUseCase(
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
    fun `maps country views to UI model`() = test {
        val forced = false
        val model = CountryViewsModel(10, 15, listOf(country), false)
        whenever(
                store.getCountryViews(
                        site,
                        statsGranularity,
                        limitMode,
                        selectedDate
                )
        ).thenReturn(model)
        whenever(store.fetchCountryViews(site, statsGranularity,
                limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            val mapItem = (this[1] as MapItem)
            assertThat(mapItem.mapData).isEqualTo("['CZ',500],")
            assertThat(mapItem.label).isEqualTo(R.string.stats_country_views_label)
            assertLabel(this[2])
            assertItem(this[3], country.fullName, country.views, country.flagIconUrl)
        }
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val model = CountryViewsModel(10, 15, listOf(country), true)
        whenever(
                store.getCountryViews(
                        site,
                        statsGranularity,
                        limitMode,
                        selectedDate
                )
        ).thenReturn(model)
        whenever(
                store.fetchCountryViews(site, statsGranularity, limitMode, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(5)
            assertLink(this[4])
        }
    }

    @Test
    fun `maps empty country views to UI model`() = test {
        val forced = false
        whenever(
                store.fetchCountryViews(site, statsGranularity, limitMode, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(CountryViewsModel(0, 0, listOf(), false))
        )

        val result = loadData(true, forced)

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
                store.fetchCountryViews(site, statsGranularity, limitMode, selectedDate, forced)
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
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_countries)
    }

    private fun assertLabel(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).leftLabel).isEqualTo(R.string.stats_country_label)
        assertThat(item.rightLabel).isEqualTo(R.string.stats_country_views_label)
    }

    private fun assertItem(
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
