package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.CommentsModel.Post
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM_WITH_ICON
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class CommentsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: CommentsStore
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: CommentsUseCase
    private val postId: Long = 10
    private val postTitle = "Post"
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val totalCount = 50
    private val blockItemCount = 6
    @Before
    fun setUp() {
        useCase = CommentsUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsSiteProvider,
                tracker,
                BLOCK
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
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

        assertThat(result.type).isEqualTo(InsightsTypes.COMMENTS)
        val tabsItem = result.data!!.assertEmptyTab(0)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)

        tabsItem.onTabSelected(1)

        val updatedResult = loadComments(true, forced)

        updatedResult.data!!.assertTabWithPosts(1)
    }

    @Test
    fun `adds link to UI model when has more posts`() = test {
        val forced = false
        val model = CommentsModel(
                listOf(Post(postId, postTitle, totalCount, url)),
                listOf(),
                hasMorePosts = true,
                hasMoreAuthors = false
        )
        whenever(insightsStore.getComments(eq(statsSiteProvider.siteModel), any())).thenReturn(model)
        whenever(insightsStore.fetchComments(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.COMMENTS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertThat(this[1] is TabsItem).isTrue()
            assertTitle(this[0])
            assertThat(this[2] is Link).isFalse()
        }

        (result.data!![1] as TabsItem).onTabSelected(1)
        val updatedResult = loadComments(true, forced)

        updatedResult.data!!.apply {
            assertThat(this).hasSize(5)
            assertTitle(this[0])
            assertThat(this[4] is Link).isTrue()
        }
    }

    @Test
    fun `adds link to UI model when has more authors`() = test {
        val forced = false
        val model = CommentsModel(
                listOf(Post(postId, postTitle, totalCount, url)),
                listOf(),
                hasMorePosts = false,
                hasMoreAuthors = true
        )
        whenever(insightsStore.getComments(eq(statsSiteProvider.siteModel), any())).thenReturn(model)
        whenever(insightsStore.fetchComments(site, LimitMode.Top(blockItemCount), forced)).thenReturn(
                OnStatsFetched(
                        model
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.COMMENTS)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertThat(this[3] is Link).isTrue()
        }
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

        assertThat(result.type).isEqualTo(InsightsTypes.COMMENTS)

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
        assertThat((headerItem as Header).leftLabel).isEqualTo(R.string.stats_comments_title_label)
        assertThat(headerItem.rightLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = this[3]
        assertThat(userItem.type).isEqualTo(LIST_ITEM)
        assertThat((userItem as ListItem).text).isEqualTo(postTitle)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
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
        assertThat((headerItem as Header).leftLabel).isEqualTo(R.string.stats_comments_author_label)
        assertThat(headerItem.rightLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = this[3]
        assertThat(userItem.type).isEqualTo(LIST_ITEM_WITH_ICON)
        assertThat((userItem as ListItemWithIcon).iconUrl).isEqualTo(avatar)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.iconStyle).isEqualTo(AVATAR)
        assertThat(userItem.text).isEqualTo(user)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
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

    private fun List<BlockListItem>.assertEmpty() {
        assertThat(this).hasSize(2)
        assertTitle(this[0])
        assertThat(this[1]).isEqualTo(Empty())
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
