package org.wordpress.android.fluxc.store.stats.time

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.CountryViewsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.CountryViewsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class CountryViewsStore
@Inject constructor(
    private val restClient: CountryViewsRestClient,
    private val sqlUtils: TimeStatsSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchCountryViews(
        site: SiteModel,
        pageSize: Int,
        granularity: StatsGranularity,
        date: Date,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        val payload = restClient.fetchCountryViews(site, granularity, date, pageSize + 1, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, granularity, date)
                OnStatsFetched(timeStatsMapper.map(payload.response, pageSize))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getCountryViews(site: SiteModel, period: StatsGranularity, pageSize: Int, date: Date): CountryViewsModel? {
        return sqlUtils.selectCountryViews(site, period, date)?.let { timeStatsMapper.map(it, pageSize) }
    }
}
