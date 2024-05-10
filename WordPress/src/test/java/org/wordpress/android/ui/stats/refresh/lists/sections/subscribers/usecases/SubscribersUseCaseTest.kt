package org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.VIEW_ALL
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState.SUCCESS
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon.IconStyle.AVATAR
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.LoadingItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.subscribers.usecases.SubscribersUseCase.SubscribersUseCaseFactory
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class SubscribersUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var store: FollowersStore

    @Mock
    lateinit var statsSinceLabelFormatter: StatsSinceLabelFormatter

    @Mock
    lateinit var statsUtils: StatsUtils

    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider

    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var tracker: AnalyticsTrackerWrapper

    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper
    private lateinit var useCaseFactory: SubscribersUseCaseFactory
    private lateinit var useCase: SubscribersUseCase
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val dateSubscribed = Date(10)
    private val sinceLabel = "4 days"
    private val totalCount = 50
    private val blockPageSize = 6
    private val viewAllPageSize = 10
    private val blockInitialMode = PagedMode(blockPageSize, false)
    private val viewAllInitialLoadMode = PagedMode(viewAllPageSize, false)
    private val viewAllMoreLoadMode = PagedMode(viewAllPageSize, true)
    private val contentDescription = "Name, Subscriber since"

    @Before
    fun setUp() {
        useCaseFactory = SubscribersUseCaseFactory(
            testDispatcher(),
            testDispatcher(),
            store,
            statsSiteProvider,
            statsSinceLabelFormatter,
            statsUtils,
            tracker,
            contentDescriptionHelper
        )
        useCase = useCaseFactory.build(BLOCK)
        whenever(statsSinceLabelFormatter.getSinceLabelLowerCase(dateSubscribed)).thenReturn(sinceLabel)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(contentDescriptionHelper.buildContentDescription(any(), any<String>(), any()))
            .thenReturn(contentDescription)
    }

    @Test
    fun `maps followers to UI model`() = test {
        val refresh = true
        val model = FollowersModel(
            totalCount,
            listOf(FollowerModel(avatar, user, url, dateSubscribed)),
            hasMore = false
        )
        whenever(store.getFollowers(site, LimitMode.Top(blockPageSize))).thenReturn(model)
        whenever(store.fetchFollowers(site, blockInitialMode)).thenReturn(OnStatsFetched(model))

        val result = loadFollowers(refresh)

        assertThat(result.state).isEqualTo(SUCCESS)
        result.data!!.assertFollowers()
    }

    @Test
    fun `maps empty followers to UI model`() = test {
        val refresh = true
        whenever(store.fetchFollowers(site, blockInitialMode))
            .thenReturn(OnStatsFetched(model = FollowersModel(0, listOf(), hasMore = false)))

        val result = loadFollowers(refresh)

        assertThat(result.state).isEqualTo(UseCaseState.EMPTY)
    }

    @Test
    fun `maps error item to UI model`() = test {
        val refresh = true
        val message = "Generic error"
        whenever(store.fetchFollowers(site, blockInitialMode))
            .thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadFollowers(refresh)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    @Test
    fun `maps followers to UI model in the view all mode`() = test {
        useCase = useCaseFactory.build(VIEW_ALL)

        val refresh = true
        val model = FollowersModel(
            totalCount,
            List(10) { FollowerModel(avatar, user, url, dateSubscribed) },
            hasMore = true
        )
        whenever(store.getFollowers(site, LimitMode.All)).thenReturn(model)
        whenever(store.fetchFollowers(site, viewAllInitialLoadMode)).thenReturn(OnStatsFetched(model))

        val updatedModel = FollowersModel(
            totalCount,
            List(11) { FollowerModel(avatar, user, url, dateSubscribed) },
            hasMore = false
        )
        whenever(store.fetchFollowers(site, viewAllMoreLoadMode, true)).thenReturn(OnStatsFetched(updatedModel))

        val result = loadFollowers(refresh)

        assertThat(result.state).isEqualTo(SUCCESS)

        var updatedResult = loadFollowers(refresh)
        val button = updatedResult.data!!.assertViewAllFollowersFirstLoad()

        useCase.liveData.observeForever { if (it != null) updatedResult = it }

        button.loadMore()
        assertThat(updatedResult.data).hasSize(14)
    }

    private suspend fun loadFollowers(refresh: Boolean, forced: Boolean = false): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_subscribers)
    }

    private fun List<BlockListItem>.assertViewAllFollowersFirstLoad(): LoadingItem {
        assertThat(this).hasSize(14)
        assertThat(this[2]).isEqualTo(Header(R.string.stats_name_label, R.string.stats_subscriber_since_label))
        val follower = this[3] as ListItemWithIcon
        assertThat(follower.iconUrl).isEqualTo(avatar)
        assertThat(follower.iconStyle).isEqualTo(AVATAR)
        assertThat(follower.text).isEqualTo(user)
        assertThat(follower.value).isEqualTo(sinceLabel)
        assertThat(follower.contentDescription).isEqualTo(contentDescription)

        assertThat(this[12] is ListItemWithIcon).isTrue()

        assertThat(this[13] is LoadingItem).isTrue()
        return this[13] as LoadingItem
    }

    private fun List<BlockListItem>.assertFollowers() {
        assertThat(this).hasSize(4)
        assertTitle(this[0])
        assertThat(this[1]).isEqualTo(Header(R.string.stats_name_label, R.string.stats_subscriber_since_label))
        val follower = this[2] as ListItemWithIcon
        assertThat(follower.iconUrl).isEqualTo(avatar)
        assertThat(follower.iconStyle).isEqualTo(AVATAR)
        assertThat(follower.text).isEqualTo(user)
        assertThat(follower.value).isEqualTo(sinceLabel)
        assertThat(follower.contentDescription).isEqualTo(contentDescription)
    }
}
