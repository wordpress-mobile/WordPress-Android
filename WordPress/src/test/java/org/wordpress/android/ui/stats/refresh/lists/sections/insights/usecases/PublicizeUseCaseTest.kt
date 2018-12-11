package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions
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
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItemWithIcon
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LINK
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.utils.ServiceMapper

class PublicizeUseCaseTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    @Mock lateinit var serviceMapper: ServiceMapper
    private lateinit var useCase: PublicizeUseCase
    private val pageSize = 5
    @Before
    fun setUp() {
        useCase = PublicizeUseCase(
                Dispatchers.Unconfined,
                insightsStore,
                serviceMapper
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

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(3)
            assertTitle(this.items[0])
            val label = this.items[1] as Label
            assertThat(label.leftLabel).isEqualTo(R.string.stats_publicize_service_label)
            assertThat(label.rightLabel).isEqualTo(R.string.stats_publicize_followers_label)
            assertThat(this.items[2]).isEqualTo(mockedItem)
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

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(4)
            assertTitle(this.items[0])
            val label = this.items[1] as Label
            assertThat(label.leftLabel).isEqualTo(R.string.stats_publicize_service_label)
            assertThat(label.rightLabel).isEqualTo(R.string.stats_publicize_followers_label)
            assertThat(this.items[2]).isEqualTo(mockedItem)
            assertLink(this.items[3])
        }
    }

    @Test
    fun `maps empty services to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchPublicizeData(site, pageSize, forced)).thenReturn(
                OnStatsFetched(PublicizeModel(listOf(), false))
        )

        val result = loadPublicizeModel(true, forced)

        Assertions.assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            Assertions.assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            Assertions.assertThat(this.items[1]).isEqualTo(Empty)
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

        Assertions.assertThat(result.type).isEqualTo(ERROR)
        (result as Error).apply {
            Assertions.assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun assertTitle(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(TITLE)
        Assertions.assertThat((item as Title).textResource).isEqualTo(R.string.stats_view_publicize)
    }

    private fun assertLink(item: BlockListItem) {
        Assertions.assertThat(item.type).isEqualTo(LINK)
        Assertions.assertThat((item as Link).text).isEqualTo(R.string.stats_insights_view_more)
    }

    private suspend fun loadPublicizeModel(refresh: Boolean, forced: Boolean): StatsBlock {
        var result: StatsBlock? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
