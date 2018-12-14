package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel
import org.wordpress.android.fluxc.model.stats.time.SearchTermsModel.SearchTerm
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.SearchTermsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LABEL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import java.util.Date

private const val pageSize = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)

class SearchTermsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: SearchTermsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    private lateinit var useCase: SearchTermsUseCase
    private val searchTerm = SearchTerm("search term", 10)
    @Before
    fun setUp() {
        useCase = SearchTermsUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                store,
                selectedDateProvider,
                statsDateFormatter
        )
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
    }

    @Test
    fun `maps search_terms to UI model`() = test {
        val forced = false
        val model = SearchTermsModel(10, 15, 0, listOf(searchTerm), false)
        whenever(store.fetchSearchTerms(site, pageSize, statsGranularity, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertTitle(this.items[0])
            assertLabel(this.items[1])
            assertItem(this.items[2], searchTerm.text, searchTerm.views)
        }
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val model = SearchTermsModel(10, 15, 0, listOf(searchTerm), true)
        whenever(
                store.fetchSearchTerms(site, pageSize, statsGranularity, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(4)
            assertTitle(this.items[0])
            assertLink(this.items[3])
        }
    }

    @Test
    fun `adds unknown item when there are encrypted terms`() = test {
        val forced = false
        val unknownSearchCount = 500
        val model = SearchTermsModel(0, 0, unknownSearchCount, listOf(searchTerm, searchTerm, searchTerm, searchTerm, searchTerm, searchTerm), false)
        whenever(
                store.fetchSearchTerms(site, pageSize, statsGranularity, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(8)
            assertTitle(this.items[0])
            assertLabel(this.items[1])
            assertItem(this.items[2], searchTerm.text, searchTerm.views)
            assertItem(this.items[3], searchTerm.text, searchTerm.views)
            assertItem(this.items[4], searchTerm.text, searchTerm.views)
            assertItem(this.items[5], searchTerm.text, searchTerm.views)
            assertItem(this.items[6], searchTerm.text, searchTerm.views)
            val unknownItem = this.items[7] as ListItemWithIcon
            assertThat(unknownItem.textResource).isEqualTo(R.string.stats_search_terms_unknown_search_terms)
            assertThat(unknownItem.value).isEqualTo(unknownSearchCount.toString())
        }
    }

    @Test
    fun `maps empty search_terms to UI model`() = test {
        val forced = false
        whenever(
                store.fetchSearchTerms(site, pageSize, statsGranularity, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(SearchTermsModel(0, 0, 0, listOf(), false))
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            Assertions.assertThat(this.items[1]).isEqualTo(Empty)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(
                store.fetchSearchTerms(site, pageSize, statsGranularity, selectedDate, forced)
        ).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            Assertions.assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun assertTitle(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(TITLE)
        Assertions.assertThat((item as Title).text).isEqualTo(R.string.stats_search_terms)
    }

    private fun assertLabel(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(LABEL)
        Assertions.assertThat((item as Label).leftLabel).isEqualTo(R.string.stats_search_terms_label)
        Assertions.assertThat(item.rightLabel).isEqualTo(R.string.stats_search_terms_views_label)
    }

    private fun assertItem(
        item: BlockListItem,
        key: String,
        views: Int?
    ) {
        Assertions.assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        Assertions.assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (views != null) {
            Assertions.assertThat(item.value).isEqualTo(views.toString())
        } else {
            Assertions.assertThat(item.value).isNull()
        }
    }

    private fun assertLink(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(LINK)
        Assertions.assertThat((item as Link).text).isEqualTo(R.string.stats_insights_view_more)
    }

    private suspend fun loadData(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
