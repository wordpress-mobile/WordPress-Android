package org.wordpress.android.ui.stats.refresh.lists.sections.granular.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel.Author
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel.Post
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.time.AuthorsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
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
import org.wordpress.android.ui.stats.refresh.lists.sections.granular.SelectedDateProvider.SelectedDate
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

private const val ITEMS_TO_LOAD = 6
private val statsGranularity = DAYS
private val selectedDate = Date(0)

class AuthorsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var store: AuthorsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var selectedDateProvider: SelectedDateProvider
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    @Mock lateinit var statsUtils: StatsUtils
    private lateinit var useCase: AuthorsUseCase
    private val firstAuthorViews = 20
    private val secondAuthorViews = 40
    private val authorWithoutPosts = Author("group1", firstAuthorViews, "group1.jpg", listOf())
    private val post = Post("Post1", "Post title", 20, "post.com")
    private val authorWithPosts = Author("group2", secondAuthorViews, "group2.jpg", listOf(post))
    private val contentDescription = "title, views"
    @InternalCoroutinesApi
    @Before
    fun setUp() {
        useCase = AuthorsUseCase(
                statsGranularity,
                Dispatchers.Unconfined,
                TEST_DISPATCHER,
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
                any<String>(),
                any()
        )).thenReturn(contentDescription)
        whenever(contentDescriptionHelper.buildContentDescription(
                any(),
                any(),
                any(),
                any<Int>()
        )).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `maps authors to UI model`() = test {
        val forced = false
        val model = AuthorsModel(10, listOf(authorWithoutPosts, authorWithPosts), false)
        val limitMode = LimitMode.Top(ITEMS_TO_LOAD)
        whenever(store.getAuthors(site, statsGranularity, limitMode, selectedDate)).thenReturn(model)
        whenever(store.fetchAuthors(site, statsGranularity, limitMode, selectedDate, forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        val expandableItem = result.data!!.assertNonExpandedList()

        expandableItem.onExpandClicked(true)

        val updatedResult = loadData(true, forced)

        updatedResult.data!!.assertExpandedList()
    }

    private fun List<BlockListItem>.assertNonExpandedList(): ExpandableItem {
        assertThat(this).isNotNull
        assertThat(this).hasSize(4)
        assertTitle(this[0])
        assertLabel(this[1])
        assertSingleItem(
                this[2],
                authorWithoutPosts.name,
                authorWithoutPosts.views,
                authorWithoutPosts.avatarUrl,
                50
        )
        return assertExpandableItem(
                this[3],
                authorWithPosts.name,
                authorWithPosts.views,
                authorWithPosts.avatarUrl,
                100
        )
    }

    private fun List<BlockListItem>.assertExpandedList(): ExpandableItem {
        assertThat(this).isNotNull
        assertThat(this).hasSize(6)
        assertTitle(this[0])
        assertLabel(this[1])
        assertSingleItem(
                this[2],
                authorWithoutPosts.name,
                authorWithoutPosts.views,
                authorWithoutPosts.avatarUrl,
                50
        )
        val expandableItem = assertExpandableItem(
                this[3],
                authorWithPosts.name,
                authorWithPosts.views,
                authorWithPosts.avatarUrl,
                100
        )
        assertSingleItem(this[4], post.title, post.views, null)
        assertThat(this[5]).isEqualTo(Divider)
        return expandableItem
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val model = AuthorsModel(10, listOf(authorWithoutPosts), true)
        whenever(store.getAuthors(site, statsGranularity, LimitMode.Top(ITEMS_TO_LOAD), selectedDate)).thenReturn(model)
        whenever(
                store.fetchAuthors(
                        site, statsGranularity, LimitMode.Top(ITEMS_TO_LOAD),
                        selectedDate, forced
                )
        ).thenReturn(
                OnStatsFetched(
                        model
                )
        )
        val result = loadData(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertLabel(this[1])
            assertSingleItem(
                    this[2],
                    authorWithoutPosts.name,
                    authorWithoutPosts.views,
                    authorWithoutPosts.avatarUrl,
                    100
            )
            assertLink(this[3])
        }
    }

    @Test
    fun `maps empty authors to UI model`() = test {
        val forced = false
        whenever(
                store.fetchAuthors(
                        site, statsGranularity, LimitMode.Top(ITEMS_TO_LOAD),
                        selectedDate, forced
                )
        ).thenReturn(
                OnStatsFetched(AuthorsModel(0, listOf(), false))
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
                store.fetchAuthors(
                        site,
                        statsGranularity,
                        LimitMode.Top(ITEMS_TO_LOAD),
                        selectedDate,
                        forced
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
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_authors)
    }

    private fun assertLabel(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).startLabel).isEqualTo(R.string.stats_author_label)
        assertThat(item.endLabel).isEqualTo(R.string.stats_author_views_label)
    }

    private fun assertSingleItem(
        item: BlockListItem,
        key: String,
        views: Int?,
        icon: String?,
        bar: Int? = null
    ) {
        assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (views != null) {
            assertThat(item.value).isEqualTo(views.toString())
        } else {
            assertThat(item.value).isNull()
        }
        if (bar != null) {
            assertThat(item.barWidth).isEqualTo(bar)
        } else {
            assertThat(item.barWidth).isNull()
        }
        assertThat(item.iconUrl).isEqualTo(icon)
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    private fun assertExpandableItem(
        item: BlockListItem,
        label: String,
        views: Int,
        icon: String?,
        bar: Int
    ): ExpandableItem {
        assertThat(item.type).isEqualTo(EXPANDABLE_ITEM)
        assertThat((item as ExpandableItem).header.text).isEqualTo(label)
        assertThat(item.header.value).isEqualTo(views.toString())
        assertThat(item.header.iconUrl).isEqualTo(icon)
        assertThat(item.header.barWidth).isEqualTo(bar)
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
