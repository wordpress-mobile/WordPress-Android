package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel.Author
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel.Post
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.AuthorsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
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
import org.wordpress.android.ui.stats.refresh.utils.StatsDateFormatter
import org.wordpress.android.ui.stats.refresh.utils.toFormattedString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val pageSize = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)

class AuthorsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: AuthorsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var statsDateFormatter: StatsDateFormatter
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: AuthorsUseCase
    private val firstAuthorViews = 50
    private val secondAuthorViews = 30
    private val authorWithoutPosts = Author("group1", firstAuthorViews, "group1.jpg", listOf())
    private val post = Post("Post1", "Post title", 20, "post.com")
    private val authorWithPosts = Author("group2", secondAuthorViews, "group2.jpg", listOf(post))
    @Before
    fun setUp() {
        useCase = AuthorsUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                store,
                selectedDateProvider,
                statsDateFormatter,
                tracker
        )
        whenever((selectedDateProvider.getSelectedDate(statsGranularity))).thenReturn(selectedDate)
    }

    @Test
    fun `maps authors to UI model`() = test {
        val forced = false
        val model = AuthorsModel(10, listOf(authorWithoutPosts, authorWithPosts), false)
        whenever(store.fetchAuthors(site, pageSize, statsGranularity, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        val expandableItem = (result as BlockList).assertNonExpandedList()

        expandableItem.onExpandClicked(true)

        val updatedResult = loadData(true, forced)

        (updatedResult as BlockList).assertExpandedList()
    }

    private fun BlockList.assertNonExpandedList(): ExpandableItem {
        Assertions.assertThat(this.items).hasSize(4)
        assertTitle(this.items[0])
        assertLabel(this.items[1])
        assertSingleItem(
                this.items[2],
                authorWithoutPosts.name,
                authorWithoutPosts.views,
                authorWithoutPosts.avatarUrl
        )
        return assertExpandableItem(
                this.items[3],
                authorWithPosts.name,
                authorWithPosts.views,
                authorWithPosts.avatarUrl
        )
    }

    private fun BlockList.assertExpandedList(): ExpandableItem {
        Assertions.assertThat(this.items).hasSize(6)
        assertTitle(this.items[0])
        assertLabel(this.items[1])
        assertSingleItem(
                this.items[2],
                authorWithoutPosts.name,
                authorWithoutPosts.views,
                authorWithoutPosts.avatarUrl
        )
        val expandableItem = assertExpandableItem(
                this.items[3],
                authorWithPosts.name,
                authorWithPosts.views,
                authorWithPosts.avatarUrl
        )
        assertSingleItem(this.items[4], post.title, post.views, null)
        Assertions.assertThat(this.items[5]).isEqualTo(Divider)
        return expandableItem
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val model = AuthorsModel(10, listOf(authorWithoutPosts), true)
        whenever(store.fetchAuthors(site, pageSize, statsGranularity, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(4)
            assertTitle(this.items[0])
            assertLabel(this.items[1])
            assertSingleItem(
                    this.items[2],
                    authorWithoutPosts.name,
                    authorWithoutPosts.views,
                    authorWithoutPosts.avatarUrl
            )
            assertLink(this.items[3])
        }
    }

    @Test
    fun `maps empty authors to UI model`() = test {
        val forced = false
        whenever(store.fetchAuthors(site, pageSize, statsGranularity, selectedDate, forced)).thenReturn(
                OnStatsFetched(AuthorsModel(0, listOf(), false))
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
                store.fetchAuthors(
                        site,
                        pageSize,
                        statsGranularity,
                        selectedDate,
                        forced
                )
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
        Assertions.assertThat((item as Title).textResource).isEqualTo(R.string.stats_authors)
    }

    private fun assertLabel(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(HEADER)
        Assertions.assertThat((item as Header).leftLabel).isEqualTo(R.string.stats_author_label)
        Assertions.assertThat(item.rightLabel).isEqualTo(R.string.stats_author_views_label)
    }

    private fun assertSingleItem(
        item: BlockListItem,
        key: String,
        views: Int?,
        icon: String?
    ) {
        Assertions.assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        Assertions.assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (views != null) {
            Assertions.assertThat(item.value).isEqualTo(views.toString())
        } else {
            Assertions.assertThat(item.value).isNull()
        }
        Assertions.assertThat(item.iconUrl).isEqualTo(icon)
    }

    private fun assertExpandableItem(
        item: BlockListItem,
        label: String,
        views: Int,
        icon: String?
    ): ExpandableItem {
        Assertions.assertThat(item.type).isEqualTo(EXPANDABLE_ITEM)
        Assertions.assertThat((item as ExpandableItem).header.text).isEqualTo(label)
        Assertions.assertThat(item.header.value).isEqualTo(views.toFormattedString())
        Assertions.assertThat(item.header.iconUrl).isEqualTo(icon)
        return item
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
