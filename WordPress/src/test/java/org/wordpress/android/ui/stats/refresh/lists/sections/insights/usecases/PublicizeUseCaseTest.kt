package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.model.stats.PublicizeModel.Service
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Header
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class PublicizeUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var serviceMapper: ServiceMapper
    @Mock lateinit var tracker: AnalyticsTrackerWrapper
    private lateinit var useCase: PublicizeUseCase
    private val pageSize = 5
    @Before
    fun setUp() {
        useCase = PublicizeUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                serviceMapper,
                tracker
        )
    }

    @Test
    fun `maps services to UI model`() = test {
        val forced = false
        val followers = 100
        val services = listOf(Service("facebook", followers))
        whenever(insightsStore.fetchPublicizeData(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        PublicizeModel(services, false)
                )
        )
        val mockedItem = mock<ListItemWithIcon>()
        whenever(serviceMapper.map(services)).thenReturn(listOf(mockedItem))

        val result = loadPublicizeModel(true, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.PUBLICIZE)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(3)
            assertTitle(this[0])
            val header = this[1] as Header
            assertThat(header.leftLabel).isEqualTo(R.string.stats_publicize_service_label)
            assertThat(header.rightLabel).isEqualTo(R.string.stats_publicize_followers_label)
            assertThat(this[2]).isEqualTo(mockedItem)
        }
    }

    @Test
    fun `trims services and adds view more link when hasMore is true`() = test {
        val forced = false
        val followers = 100
        val services = listOf(
                Service("service1", followers)
        )
        whenever(insightsStore.fetchPublicizeData(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        PublicizeModel(services, true)
                )
        )
        val mockedItem = mock<ListItemWithIcon>()
        whenever(serviceMapper.map(services.take(5))).thenReturn(listOf(mockedItem))

        val result = loadPublicizeModel(true, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.PUBLICIZE)
        assertThat(result.state).isEqualTo(UseCaseState.SUCCESS)
        result.data!!.apply {
            assertThat(this).hasSize(4)
            assertTitle(this[0])
            val header = this[1] as Header
            assertThat(header.leftLabel).isEqualTo(R.string.stats_publicize_service_label)
            assertThat(header.rightLabel).isEqualTo(R.string.stats_publicize_followers_label)
            assertThat(this[2]).isEqualTo(mockedItem)
            assertLink(this[3])
        }
    }

    @Test
    fun `maps empty services to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchPublicizeData(site, pageSize, forced)).thenReturn(
                OnStatsFetched(PublicizeModel(listOf(), false))
        )

        val result = loadPublicizeModel(true, forced)

        assertThat(result.type).isEqualTo(InsightsTypes.PUBLICIZE)
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
        whenever(insightsStore.fetchPublicizeData(site, pageSize, forced)).thenReturn(
                OnStatsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadPublicizeModel(true, forced)

        assertThat(result.state).isEqualTo(UseCaseState.ERROR)
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_publicize)
    }

    private fun assertLink(item: BlockListItem) {
        assertThat(item.type).isEqualTo(LINK)
        assertThat((item as Link).text).isEqualTo(R.string.stats_insights_view_more)
    }

    private suspend fun loadPublicizeModel(refresh: Boolean, forced: Boolean): UseCaseModel {
        var result: UseCaseModel? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
