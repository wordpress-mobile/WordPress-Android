package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import kotlinx.coroutines.Dispatchers
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
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.CommentsModel.Post
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.ItemPopupMenuHandler
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class CommentsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: CommentsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    @Mock lateinit var popupMenuHandler: ItemPopupMenuHandler
    @Mock lateinit var contentDescriptionHelper: ContentDescriptionHelper
    @Mock lateinit var statsUtils: StatsUtils
    private lateinit var useCase: CommentsUseCase
    private val postId: Long = 10
    private val postTitle = "Post"
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val totalCount = 50
    private val blockItemCount = 6
    private val contentDescription = "title, views"

    @Before
    fun setUp() {
        useCase = CommentsUseCase(
                Dispatchers.Unconfined,
                testDispatcher(),
                insightsStore,
                statsSiteProvider,
                popupMenuHandler,
                statsUtils,
                contentDescriptionHelper
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(contentDescriptionHelper.buildContentDescription(
                any(),
                any<String>(),
                any()
        )).thenReturn(contentDescription)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
    }

    @Test
    fun `maps posts comments to UI model`() = test {
        val forced = false
        val model = CommentsModel(
                listOf(Post(postId, postTitle, totalCount, url)),
                listOf(),
                hasMorePosts = false,
                hasMoreAuthors = false
        )
        whenever(insightsStore.getComments(eq(statsSiteProvider.siteModel), any())).thenReturn(model)
        whenever(insightsStore.fetchComments(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(InsightType.COMMENTS)
        val tabsItem = result.data!!.assertEmptyTab(0)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)

        tabsItem.onTabSelected(1)

        val updatedResult = loadComments(true, forced)

        updatedResult.data!!.assertTabWithPosts(1)
    }

    @Test
    fun `maps comment authors to UI model`() = test {
        val forced = false
        val model = CommentsModel(
                listOf(),
                listOf(CommentsModel.Author(user, totalCount, url, avatar)),
                hasMorePosts = false,
                hasMoreAuthors = false
        )
        whenever(insightsStore.getComments(eq(statsSiteProvider.siteModel), any())).thenReturn(model)
        whenever(insightsStore.fetchComments(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(InsightType.COMMENTS)

        val tabsItem = result.data!!.assertTabWithUsers(0)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)

        tabsItem.onTabSelected(1)

        val updatedResult = loadComments(true, forced)

        updatedResult.data!!.assertEmptyTab(1)
    }

    @Test
    fun `maps empty comments to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        CommentsModel(listOf(), listOf(), hasMorePosts = false, hasMoreAuthors = false)
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(insightsStore.fetchComments(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun List<BlockListItem>.assertTabWithPosts(position: Int): TabsItem {
        assertThat(this).hasSize(4)
        assertTitle(this[0])
        val tabsItem = this[1] as TabsItem

        assertThat(tabsItem.tabs[0]).isEqualTo(R.string.stats_comments_authors)

        assertThat(tabsItem.tabs[1]).isEqualTo(R.string.stats_comments_posts_and_pages)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)

        val headerItem = this[2]
        assertThat(headerItem.type).isEqualTo(HEADER)
        assertThat((headerItem as Header).startLabel).isEqualTo(R.string.stats_comments_title_label)
        assertThat(headerItem.endLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = this[3]
        assertThat(userItem.type).isEqualTo(LIST_ITEM)
        assertThat((userItem as ListItem).text).isEqualTo(postTitle)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
        assertThat(userItem.contentDescription).isEqualTo(contentDescription)
        return tabsItem
    }

    private fun List<BlockListItem>.assertTabWithUsers(position: Int): TabsItem {
        assertThat(this).hasSize(4)
        assertTitle(this[0])
        val tabsItem = this[1] as TabsItem

        assertThat(tabsItem.tabs[0]).isEqualTo(R.string.stats_comments_authors)

        assertThat(tabsItem.tabs[1]).isEqualTo(R.string.stats_comments_posts_and_pages)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)

        val headerItem = this[2]
        assertThat(headerItem.type).isEqualTo(HEADER)
        assertThat((headerItem as Header).startLabel).isEqualTo(R.string.stats_comments_author_label)
        assertThat(headerItem.endLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = this[3]
        assertThat(userItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((userItem as ListItemWithIcon).iconUrl).isEqualTo(avatar)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.iconStyle).isEqualTo(AVATAR)
        assertThat(userItem.text).isEqualTo(user)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
        assertThat(userItem.contentDescription).isEqualTo(contentDescription)
        return tabsItem
    }

    private fun List<BlockListItem>.assertEmptyTab(position: Int): TabsItem {
        assertThat(this).hasSize(3)
        assertTitle(this[0])
        val tabsItem = this[1] as TabsItem

        assertThat(tabsItem.tabs[0]).isEqualTo(R.string.stats_comments_authors)

        assertThat(tabsItem.tabs[1]).isEqualTo(R.string.stats_comments_posts_and_pages)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)

        assertThat(this[2]).isEqualTo(Empty())
        return tabsItem
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_comments)
    }

    private suspend fun loadComments(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }
}
