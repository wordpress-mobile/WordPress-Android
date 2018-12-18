package org.wordpress.android.ui.stats.refresh.lists.sections

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.BlockList
import org.wordpress.android.ui.stats.refresh.lists.Loading
import org.wordpress.android.ui.stats.refresh.lists.StatsBlock
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import javax.inject.Provider

class BaseStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var localDataProvider: Provider<String?>
    @Mock lateinit var remoteDataProvider: Provider<String?>
    private val localData = "local data"
    private val remoteData = "remote data"
    @Mock lateinit var site: SiteModel
    private lateinit var block: TestUseCase
    private val result = mutableListOf<StatsBlock?>()
    private val loadingBlock = Loading(ALL_TIME_STATS, listOf<BlockListItem>(Title(string.stats_insights_all_time)))

    @Before
    fun setUp() {
        block = TestUseCase(
                localDataProvider,
                remoteDataProvider,
                loadingBlock.items
        )
        whenever(localDataProvider.get()).thenReturn(localData)
        whenever(remoteDataProvider.get()).thenReturn(remoteData)
        result.clear()
        block.liveData.observeForever { result.add(it) }
    }

    @Test
    fun `on fetch loads data from DB when current value is null`() = test {
        assertThat(result).isEmpty()

        block.fetch(site, false, false)

        assertThat(result[0]).isEqualTo(loadingBlock)
        assertData(1, localData)
    }

    @Test
    fun `on fetch returns null item when DB is empty`() = test {
        assertThat(result).isEmpty()
        whenever(localDataProvider.get()).thenReturn(null)

        block.fetch(site, false, false)

        assertThat(result).startsWith(loadingBlock)
    }

    @Test
    fun `on refresh calls loads data from DB and later from API`() = test {
        assertThat(result).isEmpty()

        block.fetch(site, true, false)

        assertThat(result.size).isEqualTo(3)
        assertThat(result[0]).isEqualTo(loadingBlock)
        assertData(1, localData)
        assertData(2, remoteData)
    }

    @Test
    fun `live data value is cleared`() = test {
        block.fetch(site, false, false)

        assertThat(result[0]).isEqualTo(loadingBlock)

        assertData(1, localData)

        block.clear()

        assertThat(block.liveData.value).isEqualTo(loadingBlock)
    }

    @After
    fun tearDown() {
        block.clear()
    }

    private fun assertData(position: Int, data: String) {
        val blockList = result[position] as BlockList
        val firstItem = blockList.items[0] as Text
        assertThat(firstItem.text).isEqualTo(data)
    }

    class TestUseCase(
        private val localDataProvider: Provider<String?>,
        private val remoteDataProvider: Provider<String?>,
        private val loadingItems: List<BlockListItem>
    ) : BaseStatsUseCase<String, Int>(
            ALL_TIME_STATS,
            Dispatchers.Unconfined
    ) {
        override fun buildLoadingItem(): List<BlockListItem> {
            return loadingItems
        }

        override fun buildUiModel(domainModel: String, nullableUiState: Int?): List<BlockListItem> {
            return listOf(Text(domainModel))
        }

        override suspend fun loadCachedData(site: SiteModel) {
            localDataProvider.get()?.let { onModel(it) }
        }

        override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean) {
            val domainModel = remoteDataProvider.get()
            if (domainModel != null) {
                onModel(domainModel)
            } else {
                onEmpty()
            }
        }
    }
}
