package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSummaryUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore
) {
    private val mutex = Mutex()
    private var cachedSummary:
        Pair<Long, StatsSummaryData>? = null

    @Suppress("ReturnCount")
    suspend operator fun invoke(
        siteId: Long,
        forceRefresh: Boolean = false
    ): StatsSummaryResult {
        val token = accountStore.accessToken
        if (token.isNullOrEmpty()) {
            return StatsSummaryResult.Error(
                "No access token"
            )
        }
        statsRepository.init(token)
        mutex.withLock {
            val cached = cachedSummary
            if (!forceRefresh &&
                cached != null &&
                cached.first == siteId
            ) {
                return StatsSummaryResult
                    .Success(cached.second)
            }
        }
        val result =
            statsRepository.fetchStatsSummary(siteId)
        mutex.withLock {
            if (result is StatsSummaryResult.Success) {
                cachedSummary = siteId to result.data
            }
        }
        return result
    }

    suspend fun clearCache() {
        mutex.withLock { cachedSummary = null }
    }
}
