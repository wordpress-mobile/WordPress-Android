package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Divider
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ExpandableItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.EXPANDABLE_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.viewmodel.ResourceProvider

class TagsAndCategoriesUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var resourceProvider: ResourceProvider
    private lateinit var useCase: TagsAndCategoriesUseCase
    private val pageSize = 6
    private val singleTagViews: Long = 10
    private val firstTag = TagModel.Item("tag1", "tag", "url.com")
    private val secondTag = TagModel.Item("tag2", "tag", "url2.com")
    private val singleTag = TagModel(listOf(firstTag), singleTagViews)
    private val categoryViews: Long = 15
    @Before
    fun setUp() {
        useCase = TagsAndCategoriesUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                resourceProvider
        )
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
        whenever(insightsStore.fetchTags(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        TagsModel(listOf(singleTag, category), hasMore = false)
                )
        )

        val result = loadTags(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        val expandableItem = (result as BlockList).assertNonExpandedList(categoryName)

        expandableItem.onExpandClicked(true)

        val updatedResult = loadTags(true, forced)

        (updatedResult as BlockList).assertExpandedList(categoryName)
    }

    private fun BlockList.assertNonExpandedList(
        categoryName: String
    ): ExpandableItem {
        assertThat(this.items).hasSize(3)
        assertTitle(this.items[0])
        assertSingleTag(this.items[1], firstTag.name, singleTagViews.toString())
        return assertCategory(this.items[2], categoryName, categoryViews)
    }

    private fun BlockList.assertExpandedList(
        categoryName: String
    ): ExpandableItem {
        assertThat(this.items).hasSize(6)
        assertTitle(this.items[0])
        assertSingleTag(this.items[1], firstTag.name, singleTagViews.toString())
        val expandableItem = assertCategory(this.items[2], categoryName, categoryViews)
        assertSingleTag(this.items[3], firstTag.name, null)
        assertSingleTag(this.items[4], secondTag.name, null)
        assertThat(this.items[5]).isEqualTo(Divider)
        return expandableItem
    }

    @Test
    fun `adds view more button when hasMore`() = test {
        val forced = false
        val singleTagViews: Long = 10
        val tagItem = TagModel.Item("tag1", "tag", "url.com")
        val tag = TagModel(listOf(tagItem), singleTagViews)
        whenever(insightsStore.fetchTags(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        TagsModel(listOf(tag), hasMore = true)
                )
        )

        val result = loadTags(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(3)
            assertTitle(this.items[0])
            assertSingleTag(this.items[1], tagItem.name, singleTagViews.toString())
            assertLink(this.items[2])
        }
    }

    @Test
    fun `maps empty tags to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchTags(site, pageSize, forced)).thenReturn(
                OnStatsFetched(TagsModel(listOf(), hasMore = false))
        )

        val result = loadTags(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            assertThat(this.items[1]).isEqualTo(BlockListItem.Empty)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(insightsStore.fetchTags(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadTags(true, forced)

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_tags_and_categories)
    }

    private fun assertSingleTag(
        item: BlockListItem,
        key: String,
        label: String?
    ) {
        assertThat(item.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((item as ListItemWithIcon).text).isEqualTo(key)
        if (label != null) {
            assertThat(item.value).isEqualTo(label)
        } else {
            assertThat(item.value).isNull()
        }
        assertThat(item.icon).isEqualTo(R.drawable.ic_tag_grey_dark_24dp)
    }

    private fun assertCategory(
        item: BlockListItem,
        label: String,
        views: Long
    ): ExpandableItem {
        assertThat(item.type).isEqualTo(EXPANDABLE_ITEM)
        assertThat((item as ExpandableItem).header.text).isEqualTo(label)
        assertThat(item.header.value).isEqualTo(views.toString())
        assertThat(item.header.icon).isEqualTo(R.drawable.ic_folder_multiple_grey_dark_24dp)
        return item
    }

    private fun assertLink(item: BlockListItem) {
        assertThat(item.type).isEqualTo(LINK)
        assertThat((item as Link).text).isEqualTo(R.string.stats_insights_view_more)
    }

    private suspend fun loadTags(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
