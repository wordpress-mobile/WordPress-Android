package org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem
import org.wordpress.android.ui.stats.refresh.lists.Loading
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsBlock
import javax.inject.Provider

class BaseStatsBlockTest : BaseUnitTest() {
    @Mock lateinit var localDataProvider: Provider<StatsListItem?>
    @Mock lateinit var remoteDataProvider: Provider<StatsListItem>
    @Mock lateinit var localData: StatsListItem
    @Mock lateinit var remoteData: StatsListItem
    @Mock lateinit var site: SiteModel
    private lateinit var block: TestBlock

    @Before
    fun setUp() {
        block = TestBlock(
                localDataProvider,
                remoteDataProvider
        )
        whenever(localDataProvider.get()).thenReturn(localData)
        whenever(remoteDataProvider.get()).thenReturn(remoteData)
    }

    /**
     * suspend fun fetch(site: SiteModel, refresh: Boolean, forced: Boolean) {
    if (liveData.value == null) {
    mutableLiveData.postValue(loadCachedData(site) ?: BlockList(listOf(Empty)))
    }
    if (refresh) {
    mutableLiveData.postValue(fetchRemoteData(site, refresh, forced))
    }
    }
     */

    @Test
    fun `on fetch loads data from DB when current value is null`() = test {
        assertThat(block.liveData.value).isNull()

        block.fetch(site, false, false)

        assertThat(block.liveData.value).isEqualTo(localData)
    }

    @Test
    fun `on fetch returns loading item when DB is empty`() = test {
        assertThat(block.liveData.value).isNull()
        whenever(localDataProvider.get()).thenReturn(null)

        block.fetch(site, false, false)

        assertThat(block.liveData.value).isEqualTo(
                Loading(
                        ALL_TIME_STATS
                )
        )
    }

    @Test
    fun `on refresh calls loads data from DB and later from API`() = test {
        val result = mutableListOf<StatsListItem?>()
        assertThat(block.liveData.value).isNull()

        block.liveData.observeForever { result.add(it) }

        block.fetch(site, true, false)

        assertThat(result.size).isEqualTo(2)
        assertThat(result[0]).isEqualTo(localData)
        assertThat(result[1]).isEqualTo(remoteData)
    }

    @Test
    fun `live data value is cleared`() = test {
        block.fetch(site, false, false)

        assertThat(block.liveData.value).isEqualTo(localData)

        block.clear()

        assertThat(block.liveData.value).isNull()
    }

    class TestBlock(
        private val localDataProvider: Provider<StatsListItem?>,
        private val remoteDataProvider: Provider<StatsListItem>
    ) : BaseStatsBlock(
            ALL_TIME_STATS,
            Dispatchers.Unconfined
    ) {
        override suspend fun loadCachedData(site: SiteModel): StatsListItem? {
            return localDataProvider.get()
        }

        override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): StatsListItem {
            return remoteDataProvider.get()
        }
    }
}
