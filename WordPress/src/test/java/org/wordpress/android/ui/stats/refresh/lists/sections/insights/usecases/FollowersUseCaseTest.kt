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
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Information
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Date

class FollowersUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var statsUtilsWrapper: StatsUtilsWrapper
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var statsSiteProvider: StatsSiteProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
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
                statsSiteProvider,
                statsUtilsWrapper,
                resourceProvider,
                tracker
        )
        whenever(statsUtilsWrapper.getSinceLabelLowerCase(dateSubscribed)).thenReturn(sinceLabel)
        whenever(resourceProvider.getString(any())).thenReturn(wordPressLabel)
        whenever(resourceProvider.getString(eq(R.string.stats_followers_count_message), any(), any())).thenReturn(
                message
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
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

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        val tabsItem = result.data!!.assertSelectedFollowers(position = 0)

        tabsItem.onTabSelected(1)

        val updatedResult = loadFollowers(refresh, forced)

        updatedResult.data!!.assertEmptyTabSelected(1)
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

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        val tabsItem = result.data!!.assertEmptyTabSelected(0)

        tabsItem.onTabSelected(1)
        val updatedResult = loadFollowers(refresh, forced)
        updatedResult.data!!.assertSelectedFollowers(position = 1)
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

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
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

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
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

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private suspend fun loadFollowers(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        return checkNotNull(result)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_followers)
    }

    private fun List<BlockListItem>.assertSelectedFollowers(position: Int): TabsItem {
        assertThat(this).hasSize(5)
        assertTitle(this[0])
        val tabsItem = this[1] as TabsItem
        assertThat(tabsItem.tabs[0]).isEqualTo(R.string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(R.string.stats_followers_email)
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(this[2]).isEqualTo(Information("Total followers count is 50"))
        assertThat(this[3]).isEqualTo(
                Header(
                        string.stats_follower_label,
                        string.stats_follower_since_label
                )
        )
        val follower = this[4] as ListItemWithIcon
        assertThat(follower.iconUrl).isEqualTo(avatar)
        assertThat(follower.iconStyle).isEqualTo(AVATAR)
        assertThat(follower.text).isEqualTo(user)
        assertThat(follower.value).isEqualTo(sinceLabel)
        assertThat(follower.showDivider).isEqualTo(false)
        return tabsItem
    }

    private fun List<BlockListItem>.assertEmptyTabSelected(position: Int): TabsItem {
        assertThat(this).hasSize(3)
        assertTitle(this[0])
        val tabsItem = this[1] as TabsItem
        assertThat(tabsItem.selectedTabPosition).isEqualTo(position)
        assertThat(tabsItem.tabs[0]).isEqualTo(R.string.stats_followers_wordpress_com)
        assertThat(tabsItem.tabs[1]).isEqualTo(R.string.stats_followers_email)
        assertThat(this[2]).isEqualTo(Empty())
        return tabsItem
    }

    private fun UseCaseModel.assertEmpty() {
        val nonNullData = this.data!!
        assertThat(nonNullData).hasSize(2)
        assertTitle(nonNullData[0])
        assertThat(nonNullData[1]).isEqualTo(Empty())
    }
}
