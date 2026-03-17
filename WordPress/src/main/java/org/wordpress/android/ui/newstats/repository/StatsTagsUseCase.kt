package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsTagsUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore
) {
    private val mutex = Mutex()
    private var cachedTags:
        Triple<Long, Int, StatsTagsData>? = null

    @Suppress("ReturnCount")
    suspend operator fun invoke(
        siteId: Long,
        max: Int = DEFAULT_MAX_ITEMS,
        forceRefresh: Boolean = false
    ): TagsResult {
        val token = accountStore.accessToken
        if (token.isNullOrEmpty()) {
            return TagsResult.Error("No access token")
        }
        statsRepository.init(token)

        // Check cache under lock, but fetch outside
        // to avoid blocking other callers during
        // network requests.
        val cached = mutex.withLock { cachedTags }
        if (!forceRefresh &&
            isCacheHit(cached, siteId, max)
        ) {
            return TagsResult.Success(cached!!.third)
        }

        val result = statsRepository.fetchTags(
            siteId = siteId,
            max = max
        )
        mutex.withLock {
            if (result is TagsResult.Success) {
                cachedTags =
                    Triple(siteId, max, result.data)
            }
        }
        return result
    }

    private fun isCacheHit(
        cached: Triple<Long, Int, StatsTagsData>?,
        siteId: Long,
        max: Int
    ): Boolean = cached != null &&
        cached.first == siteId &&
        cached.second == max

    companion object {
        private const val DEFAULT_MAX_ITEMS = 10
    }
}
