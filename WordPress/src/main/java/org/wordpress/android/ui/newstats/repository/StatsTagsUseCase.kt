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
        return mutex.withLock {
            val cached = cachedTags
            if (!forceRefresh &&
                cached != null &&
                cached.first == siteId &&
                cached.second == max
            ) {
                return@withLock TagsResult
                    .Success(cached.third)
            }
            val result = statsRepository.fetchTags(
                siteId = siteId,
                max = max
            )
            if (result is TagsResult.Success) {
                cachedTags =
                    Triple(siteId, max, result.data)
            }
            result
        }
    }

    companion object {
        private const val DEFAULT_MAX_ITEMS = 10
    }
}
