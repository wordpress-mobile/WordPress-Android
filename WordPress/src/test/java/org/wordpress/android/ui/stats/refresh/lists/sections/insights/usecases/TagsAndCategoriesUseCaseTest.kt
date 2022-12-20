package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.TagsStore
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
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class TagsAndCategoriesUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: TagsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsUtils: StatsUtils
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    private lateinit var useCase: TagsAndCategoriesUseCase
    private val blockItemCount = 6
    private val singleTagViews: Long = 10
    private val firstTag = TagModel.Item("tag1", "tag", "url.com")
    private val secondTag = TagModel.Item("tag2", "tag", "url2.com")
    private val singleTag = TagModel(listOf(firstTag), singleTagViews)
    private val categoryViews: Long = 20
    private val contentDescription = "title, views"

    @Before
    fun setUp() {
        useCase = TagsAndCategoriesUseCase(
                testDispatcher(),
                testDispatcher(),
                insightsStore,
                statsSiteProvider,
                resourceProvider,
                statsUtils,
                tracker,
                popupMenuHandler,
                contentDescriptionHelper,
                BLOCK
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(contentDescriptionHelper.buildContentDescription(
                any(),
                any<String>(),
                any()
        )).thenReturn(contentDescription)
        whenever(contentDescriptionHelper.buildContentDescription(
                any(),
                any<String>()
        )).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Long>(), any())).then { (it.arguments[0] as Long).toString() }
    }

    @Test
    fun `maps tags to UI model`() = test {
        val forced = false
        val categoryName = "category name"
        whenever(resourceProvider.getString(eq(R.string.stats_category_folded_name), any(), any())).thenReturn(
                categoryName
        )
        val category = TagModel(
                listOf(firstTag, secondTag),
                categoryViews
        )
        val model = TagsModel(listOf(singleTag, category), hasMore = false)
        whenever(insightsStore.getTags(site, LimitMode.Top(blockItemCount))).thenReturn(model)
        whenever(insightsStore.fetchTags(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadTags(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        val expandableItem = result.data!!.assertNonExpandedList(categoryName)

        expandableItem.onExpandClicked(true)

        val updatedResult = loadTags(true, forced)

        updatedResult.data!!.assertExpandedList(categoryName)
    }

    private fun List<BlockListItem>.assertNonExpandedList(
        categoryName: String
    ): ExpandableItem {
        assertThat(this).hasSize(4)
        assertTitle(this[0])
        assertHeader(this[1])
        assertSingleTag(this[2], firstTag.name, singleTagViews.toString(), 50)
        return assertCategory(this[3], categoryName, categoryViews, 100)
    }

    private fun List<BlockListItem>.assertExpandedList(
        categoryName: String
    ): ExpandableItem {
        assertThat(this).hasSize(7)
        assertTitle(this[0])
        assertHeader(this[1])
        assertSingleTag(this[2], firstTag.name, singleTagViews.toString(), 50)
        val expandableItem = assertCategory(this[3], categoryName, categoryViews, 100)
        assertSingleTag(this[4], firstTag.name, null)
        assertSingleTag(this[5], secondTag.name, null)
        assertThat(this[6]).isEqualTo(Divider)
        return expandableItem
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val singleTagViews: Long = 10
        val tagItem = TagModel.Item("tag1", "tag", "url.com")
        val tag = TagModel(listOf(tagItem), singleTagViews)
        val model = TagsModel(listOf(tag), hasMore = true)
        whenever(insightsStore.getTags(site, LimitMode.Top(blockItemCount))).thenReturn(model)
        whenever(insightsStore.fetchTags(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadTags(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertHeader(this[1])
            assertSingleTag(this[2], tagItem.name, singleTagViews.toString(), 100)
            assertLink(this[3])
        }
    }

    @Test
    fun `maps empty tags to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchTags(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(TagsModel(listOf(), hasMore = false))
        )

        val result = loadTags(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
        result.stateData!!.apply {
            assertThat(this).hasSize(2)
            assertTitle(this[0])
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(insightsStore.fetchTags(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadTags(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_insights_tags_and_categories)
    }

    private fun assertHeader(item: BlockListItem) {
        assertThat(item.type).isEqualTo(HEADER)
        assertThat((item as Header).startLabel).isEqualTo(R.string.stats_tags_and_categories_title_label)
        assertThat(item.endLabel).isEqualTo(R.string.stats_tags_and_categories_views_label)
    }

    private fun assertSingleTag(
        item: BlockListItem,
        key: String,
        label: String?,
        bar: Int? = null
    ) {
        assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (label != null) {
            assertThat(item.value).isEqualTo(label)
        } else {
            assertThat(item.value).isNull()
        }
        if (bar != null) {
            assertThat(item.barWidth).isEqualTo(bar)
        } else {
            assertThat(item.barWidth).isNull()
        }
        assertThat(item.icon).isEqualTo(R.drawable.ic_tag_white_24dp)
        assertThat(item.contentDescription).isEqualTo(contentDescription)
    }

    private fun assertCategory(
        item: BlockListItem,
        label: String,
        views: Long,
        bar: Int
    ): ExpandableItem {
        assertThat(item.type).isEqualTo(EXPANDABLE_ITEM)
        assertThat((item as ExpandableItem).header.text).isEqualTo(label)
        assertThat(item.header.value).isEqualTo(views.toString())
        assertThat(item.header.icon).isEqualTo(R.drawable.ic_folder_multiple_white_24dp)
        assertThat(item.header.barWidth).isEqualTo(bar)
        assertThat(item.header.contentDescription).isEqualTo(contentDescription)
        return item
    }

    private fun assertLink(item: BlockListItem) {
        assertThat(item.type).isEqualTo(LINK)
        assertThat((item as Link).text).isEqualTo(R.string.stats_insights_view_more)
    }

    private suspend fun loadTags(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
