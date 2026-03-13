package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsInsightsUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore
) {
    private val mutex = Mutex()
    private var cachedInsights:
        Pair<Long, StatsInsightsData>? = null

    suspend operator fun invoke(
        siteId: Long,
        forceRefresh: Boolean = false
    ): InsightsResult {
        val token = accountStore.accessToken
        if (token.isNullOrEmpty()) {
            return InsightsResult.Error(
                "No access token"
            )
        }
        statsRepository.init(token)
        return mutex.withLock {
            val cached = cachedInsights
            if (!forceRefresh &&
                cached != null &&
                cached.first == siteId
            ) {
                return@withLock InsightsResult
                    .Success(cached.second)
            }
            val result =
                statsRepository.fetchInsights(siteId)
            if (result is InsightsResult.Success) {
                cachedInsights = siteId to result.data
            }
            result
        }
    }
}
