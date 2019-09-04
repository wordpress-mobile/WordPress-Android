package org.wordpress.android.ui.stats.refresh.lists.sections

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel
import org.wordpress.android.ui.stats.refresh.lists.sections.BaseStatsUseCase.UseCaseModel.UseCaseState
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Text
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import javax.inject.Provider

@InternalCoroutinesApi
class BaseStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var localDataProvider: Provider<String?>
    @Mock lateinit var remoteDataProvider: Provider<String?>
    private val localData = "local data"
    private val remoteData = "remote data"
    @Mock lateinit var site: SiteModel
    private lateinit var block: TestUseCase
    private val result = mutableListOf<UseCaseModel?>()
    private val loadingData = listOf<BlockListItem>(Title(R.string.stats_insights_all_time))

    @Before
    fun setUp() {
        block = TestUseCase(
                localDataProvider,
                remoteDataProvider,
                loadingData
        )
        whenever(localDataProvider.get()).thenReturn(localData)
        whenever(remoteDataProvider.get()).thenReturn(remoteData)
        result.clear()
        block.liveData.observeForever { result.add(it) }
    }

    @Test
    fun `on fetch loads data from DB when current value is null`() = test {
        assertThat(result).isEmpty()

        block.fetch(false, false)

        assertData(1, localData)
    }

    @Test
    fun `on fetch returns null item when DB is empty`() = test {
        assertThat(result).isEmpty()
        whenever(localDataProvider.get()).thenReturn(null)

        block.fetch(false, false)

        assertThat(result).hasSize(1)
        assertThat(result[0]!!.data).isNull()
        assertThat(result[0]!!.state).isEqualTo(UseCaseState.SUCCESS)
    }

    @Test
    fun `on refresh calls loads data from DB and later from API`() = test {
        assertThat(result).isEmpty()

        block.fetch(true, false)

        assertThat(result.size).isEqualTo(2)
        assertData(0, localData)
        assertThat(result[1]?.state).isEqualTo(UseCaseState.SUCCESS)
    }

    @Test
    fun `live data value is cleared`() = test {
        block.fetch(false, false)

        assertData(1, localData)

        block.clear()

        assertThat(block.liveData.value?.state).isEqualTo(UseCaseState.LOADING)
    }

    @After
    fun tearDown() {
        block.clear()
    }

    private fun assertData(position: Int, data: String) {
        val blockList = result[position]
        val firstItem = blockList?.data!![0] as Text
        assertThat(firstItem.text).isEqualTo(data)
    }

    @InternalCoroutinesApi
    class TestUseCase(
        private val localDataProvider: Provider<String?>,
        private val remoteDataProvider: Provider<String?>,
        private val loadingItems: List<BlockListItem>
    ) : BaseStatsUseCase<String, Int>(
            ALL_TIME_STATS,
            Dispatchers.Unconfined,
            TEST_DISPATCHER,
            0,
            listOf()
    ) {
        override fun buildLoadingItem(): List<BlockListItem> {
            return loadingItems
        }

        override fun buildUiModel(domainModel: String, uiState: Int): List<BlockListItem> {
            return listOf(Text(domainModel))
        }

        override suspend fun loadCachedData(): String? {
            return localDataProvider.get()
        }

        override suspend fun fetchRemoteData(forced: Boolean): State<String> {
            val domainModel = remoteDataProvider.get()
            return if (domainModel != null) {
                State.Data(domainModel)
            } else {
                State.Empty()
            }
        }
    }
}
