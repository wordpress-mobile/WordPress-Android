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
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.subscribers.PostsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.SortField
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.subscribers.EmailsStore
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseMode.BLOCK
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListHeader
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_HEADER
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.ContentDescriptionHelper
import org.wordpress.android.ui.stats.refresh.utils.StatsSiteProvider
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class EmailsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var emailsStore: EmailsStore

    @Mock
    lateinit var statsSiteProvider: StatsSiteProvider

    @Mock
    lateinit var statsUtils: StatsUtils

    @Mock
    lateinit var site: SiteModel

    @Mock
    lateinit var tracker: AnalyticsTrackerWrapper

    @Mock
    lateinit var contentDescriptionHelper: ContentDescriptionHelper

    private lateinit var useCase: EmailsUseCase
    private val itemsToLoad = 30
    private val firstPost = PostsModel.PostModel(1, "post1", "url.com", 10, 20)
    private val secondPost = PostsModel.PostModel(2, "post2", "url2.com", 30, 40)
    private val contentDescription = "latest emails, opens, clicks"

    @Before
    fun setUp() {
        useCase = EmailsUseCase(
            testDispatcher(),
            testDispatcher(),
            emailsStore,
            statsSiteProvider,
            statsUtils,
            contentDescriptionHelper,
            tracker,
            BLOCK
        )
        whenever(statsSiteProvider.siteModel).thenReturn(site)
        whenever(statsUtils.toFormattedString(any<Int>(), any())).then { (it.arguments[0] as Int).toString() }
        whenever(contentDescriptionHelper.buildContentDescription(any(), any<String>(), any<String>(), any<String>()))
            .thenReturn(contentDescription)
    }

    @Test
    fun `maps emails summary to UI model`() = test {
        val forced = false
        val model = PostsModel(listOf(firstPost, secondPost))
        whenever(emailsStore.getEmails(site, LimitMode.Top(itemsToLoad), SortField.POST_ID)).thenReturn(model)
        whenever(emailsStore.fetchEmails(site, LimitMode.Top(itemsToLoad), SortField.POST_ID, forced))
            .thenReturn(OnStatsFetched(model))

        val result = loadPosts(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            assertListHeader(this[1])
            assertListItem(this[2], firstPost.title, firstPost.opens, firstPost.clicks)
            assertListItem(this[3], secondPost.title, secondPost.opens, secondPost.clicks)
        }
    }

    @Test
    fun `maps empty posts to UI model`() = test {
        val forced = false
        whenever(emailsStore.fetchEmails(site, LimitMode.Top(itemsToLoad), SortField.POST_ID, forced)).thenReturn(
            OnStatsFetched(PostsModel(listOf()))
        )

        val result = loadPosts(true, forced)

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
        whenever(emailsStore.fetchEmails(site, LimitMode.Top(itemsToLoad), SortField.POST_ID, forced))
            .thenReturn(OnStatsFetched(StatsError(GENERIC_ERROR, message)))

        val result = loadPosts(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_emails)
    }

    private fun assertListHeader(item: BlockListItem) {
        assertThat(item.type).isEqualTo(LIST_HEADER)
        assertThat((item as ListHeader).label).isEqualTo(R.string.stats_emails_latest_emails_label)
        assertThat(item.valueLabel1).isEqualTo(R.string.stats_emails_opens_label)
        assertThat(item.valueLabel2).isEqualTo(R.string.stats_emails_clicks_label)
    }

    private fun assertListItem(blockListItem: BlockListItem, text: String, value1: Int, value2: Int) {
        assertThat(blockListItem.type).isEqualTo(BlockListItem.Type.LIST_ITEM_WITH_TWO_VALUES)
        val item = blockListItem as BlockListItem.ListItemWithTwoValues
        assertThat(item.text).isEqualTo(text)
        assertThat(item.value1).isEqualTo(value1.toString())
        assertThat(item.value2).isEqualTo(value2.toString())
    }

    private suspend fun loadPosts(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(refresh, forced)
        advanceUntilIdle()
        return checkNotNull(result)
    }
}
