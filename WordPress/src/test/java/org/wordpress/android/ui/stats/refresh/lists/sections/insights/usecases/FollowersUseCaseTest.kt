package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Error
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock.Type.ERROR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.UserItem
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

class FollowersUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var statsUtilsWrapper: StatsUtilsWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: FollowersUseCase
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val dateSubscribed = Date(10)
    private val sinceLabel = "4 days"
    private val totalCount = 50
    private val wordPressLabel = "wordpress"
    private val pageSize = 6
    val message = "Total followers count is 50"
    @Before
    fun setUp() {
        useCase = FollowersUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                statsUtilsWrapper,
                resourceProvider
        )
        whenever(statsUtilsWrapper.getSinceLabelLowerCase(dateSubscribed)).thenReturn(sinceLabel)
        whenever(resourceProvider.getString(any())).thenReturn(wordPressLabel)
        whenever(resourceProvider.getString(eq(R.string.stats_followers_count_message), any(), any())).thenReturn(
                message
        )
    }

    @Test
    fun `maps followers from selected tab to UI model and select empty tab`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        FollowersModel(
                                totalCount,
                                listOf(FollowerModel(avatar, user, url, dateSubscribed)),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        val tabsItem = (result as BlockList).assertSelectedFollowers(position = 0)

        tabsItem.onTabSelected(1)

        val updatedResult = loadFollowers(refresh, forced)

        (updatedResult as BlockList).assertEmptyTabSelected(1)
    }

    @Test
    fun `maps email followers to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        FollowersModel(
                                totalCount,
                                listOf(FollowerModel(avatar, user, url, dateSubscribed)),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        val tabsItem = (result as BlockList).assertEmptyTabSelected(0)

        tabsItem.onTabSelected(1)
        val updatedResult = loadFollowers(refresh, forced)
        (updatedResult as BlockList).assertSelectedFollowers(position = 1)
    }

    @Test
    fun `maps empty followers to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).assertEmpty()
    }

    @Test
    fun `maps WPCOM error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    @Test
    fun `maps email error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadFollowers(refresh, forced)

        assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private suspend fun loadFollowers(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).text).isEqualTo(R.string.stats_view_followers)
    }

    private fun BlockList.assertSelectedFollowers(position: Int): TabsItem {
        assertThat(this.items).hasSize(5)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem
        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_followers_email)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(this.items[2]).isEqualTo(Information("Total followers count is 50"))
        assertThat(this.items[3]).isEqualTo(
                Header(
                        string.stats_follower_label,
                        string.stats_follower_since_label
                )
        )
        val follower = this.items[4] as UserItem
        assertThat(follower.avatarUrl).isEqualTo(avatar)
        assertThat(follower.text).isEqualTo(user)
        assertThat(follower.value).isEqualTo(sinceLabel)
        assertThat(follower.showDivider).isEqualTo(false)
        return tabsItem
    }

    private fun BlockList.assertEmptyTabSelected(position: Int): TabsItem {
        assertThat(this.items).hasSize(3)
        assertTitle(this.items[0])
        val tabsItem = this.items[1] as TabsItem
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(tabsItem.tabs[0]).isEqualTo(string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(string.stats_followers_email)
        assertThat(this.items[2]).isEqualTo(Empty)
        return tabsItem
    }

    private fun BlockList.assertEmpty() {
        assertThat(this.items).hasSize(2)
        assertTitle(this.items[0])
        assertThat(this.items[1]).isEqualTo(Empty)
    }
}
