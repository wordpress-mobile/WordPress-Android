package org.wordpress.android.ui.stats.refresh.sections.insights.usecases

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
import org.wordpress.android.ui.stats.refresh.InsightsItem
import org.wordpress.android.ui.stats.refresh.Loading
import org.wordpress.android.ui.stats.refresh.sections.BaseStatsUseCase
import javax.inject.Provider

class BaseStatsUseCaseTest : BaseUnitTest() {
    @Mock lateinit var localDataProvider: Provider<InsightsItem?>
    @Mock lateinit var remoteDataProvider: Provider<InsightsItem>
    @Mock lateinit var localData: InsightsItem
    @Mock lateinit var remoteData: InsightsItem
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: TestUseCase

    @Before
    fun setUp() {
        useCase = TestUseCase(
                localDataProvider,
                remoteDataProvider
        )
        whenever(localDataProvider.get()).thenReturn(localData)
        whenever(remoteDataProvider.get()).thenReturn(remoteData)
    }

    /**
     * suspend fun fetch(site: SiteModel, refresh: Boolean, forced: Boolean) {
    if (liveData.value == null) {
    mutableLiveData.postValue(loadCachedData(site) ?: ListInsightItem(listOf(Empty)))
    }
    if (refresh) {
    mutableLiveData.postValue(fetchRemoteData(site, refresh, forced))
    }
    }
     */

    @Test
    fun `on fetch loads data from DB when current value is null`() = test {
        assertThat(useCase.liveData.value).isNull()

        useCase.fetch(site, false, false)

        assertThat(useCase.liveData.value).isEqualTo(localData)
    }

    @Test
    fun `on fetch returns loading item when DB is empty`() = test {
        assertThat(useCase.liveData.value).isNull()
        whenever(localDataProvider.get()).thenReturn(null)

        useCase.fetch(site, false, false)

        assertThat(useCase.liveData.value).isEqualTo(Loading(ALL_TIME_STATS))
    }

    @Test
    fun `on refresh calls loads data from DB and later from API`() = test {
        val result = mutableListOf<InsightsItem?>()
        assertThat(useCase.liveData.value).isNull()

        useCase.liveData.observeForever { result.add(it) }

        useCase.fetch(site, true, false)

        assertThat(result.size).isEqualTo(2)
        assertThat(result[0]).isEqualTo(localData)
        assertThat(result[1]).isEqualTo(remoteData)
    }

    @Test
    fun `live data value is cleared`() = test {
        useCase.fetch(site, false, false)

        assertThat(useCase.liveData.value).isEqualTo(localData)

        useCase.clear()

        assertThat(useCase.liveData.value).isNull()
    }

    class TestUseCase(
        private val localDataProvider: Provider<InsightsItem?>,
        private val remoteDataProvider: Provider<InsightsItem>
    ) : BaseStatsUseCase(
            ALL_TIME_STATS,
            Dispatchers.Unconfined
    ) {
        override suspend fun loadCachedData(site: SiteModel): InsightsItem? {
            return localDataProvider.get()
        }

        override suspend fun fetchRemoteData(site: SiteModel, forced: Boolean): InsightsItem {
            return remoteDataProvider.get()
        }
    }
}
