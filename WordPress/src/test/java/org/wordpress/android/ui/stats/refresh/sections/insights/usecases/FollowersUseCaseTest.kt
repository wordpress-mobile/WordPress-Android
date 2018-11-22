package org.wordpress.android.ui.stats.refresh.sections.insights.usecases

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
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.StatsUtilsWrapper
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Type.INFO
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Type.LABEL
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.Type.USER_ITEM
import org.wordpress.android.ui.stats.refresh.sections.BlockListItem.UserItem
import org.wordpress.android.ui.stats.refresh.sections.Failed
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.sections.StatsListItem.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.sections.ListInsightItem
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
    fun `maps WPCOM followers to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        FollowersModel(
                                totalCount,
                                listOf(FollowerModel(avatar, user, url, dateSubscribed)),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as ListInsightItem).apply {
            Assertions.assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            val tabsItem = this.items[1] as TabsItem

            assertThat(tabsItem.tabs[0].title).isEqualTo(string.stats_followers_wordpress_com)
            assertTabWithFollower(tabsItem.tabs[0])

            assertThat(tabsItem.tabs[1].title).isEqualTo(string.stats_followers_email)
            assertThat(tabsItem.tabs[1].items).containsOnly(Empty)
        }
    }

    @Test
    fun `maps email followers to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        FollowersModel(
                                totalCount,
                                listOf(FollowerModel(avatar, user, url, dateSubscribed)),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as ListInsightItem).apply {
            Assertions.assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            val tabsItem = this.items[1] as TabsItem

            assertThat(tabsItem.tabs[0].title).isEqualTo(string.stats_followers_wordpress_com)
            assertThat(tabsItem.tabs[0].items).containsOnly(Empty)

            assertThat(tabsItem.tabs[1].title).isEqualTo(string.stats_followers_email)
            assertTabWithFollower(tabsItem.tabs[1])
        }
    }

    @Test
    fun `maps empty followers to UI model`() = test {
        val forced = false
        val refresh = true
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as ListInsightItem).apply {
            Assertions.assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            val tabsItem = this.items[1] as TabsItem

            assertThat(tabsItem.tabs[0].title).isEqualTo(string.stats_followers_wordpress_com)
            assertThat(tabsItem.tabs[0].items).containsOnly(Empty)

            assertThat(tabsItem.tabs[1].title).isEqualTo(string.stats_followers_email)
            assertThat(tabsItem.tabs[1].items).containsOnly(Empty)
        }
    }

    @Test
    fun `maps WPCOM error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )

        val result = loadFollowers(refresh, forced)

        assertThat(result.type).isEqualTo(FAILED)
        (result as Failed).apply {
            assertThat(this.failedType).isEqualTo(R.string.stats_view_followers)
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    @Test
    fun `maps email error item to UI model`() = test {
        val forced = false
        val refresh = true
        val message = "Generic error"
        whenever(insightsStore.fetchWpComFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        model = FollowersModel(
                                0,
                                listOf(),
                                hasMore = false
                        )
                )
        )
        whenever(insightsStore.fetchEmailFollowers(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadFollowers(refresh, forced)

        assertThat(result.type).isEqualTo(FAILED)
        (result as Failed).apply {
            assertThat(this.failedType).isEqualTo(R.string.stats_view_followers)
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private suspend fun loadFollowers(refresh: Boolean, forced: Boolean): StatsListItem {
        var result: StatsListItem? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }

    private fun assertTabWithFollower(tab: TabsItem.Tab) {
        val infoItem = tab.items[0]
        assertThat(infoItem.type).isEqualTo(INFO)
        assertThat((infoItem as Information).text).isEqualTo(message)

        val labelItem = tab.items[1]
        assertThat(labelItem.type).isEqualTo(LABEL)
        assertThat((labelItem as Label).leftLabel).isEqualTo(R.string.stats_follower_label)
        assertThat(labelItem.rightLabel).isEqualTo(R.string.stats_follower_since_label)

        val userItem = tab.items[2]
        assertThat(userItem.type).isEqualTo(USER_ITEM)
        assertThat((userItem as UserItem).avatarUrl).isEqualTo(avatar)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.text).isEqualTo(user)
        assertThat(userItem.value).isEqualTo(sinceLabel)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).text).isEqualTo(R.string.stats_view_followers)
    }
}
