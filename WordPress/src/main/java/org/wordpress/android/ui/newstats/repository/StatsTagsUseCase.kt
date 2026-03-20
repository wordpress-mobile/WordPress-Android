package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import kotlin.coroutines.cancellation.CancellationException
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

    // In-flight request keyed by (siteId, max).
    // Concurrent callers with the same params join
    // the existing request instead of duplicating it.
    private var inFlight:
        Pair<Pair<Long, Int>,
            CompletableDeferred<TagsResult>>? = null

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
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

        val key = siteId to max

        // Under lock: check cache, then check/create
        // an in-flight deferred. The actual network
        // call runs outside the lock so concurrent
        // callers with different params aren't blocked.
        val (deferred, isOwner) = mutex.withLock {
            val cached = cachedTags
            if (!forceRefresh &&
                isCacheHit(cached, siteId, max)
            ) {
                return TagsResult.Success(
                    cached!!.third
                )
            }

            val existing = inFlight
            if (!forceRefresh &&
                existing != null &&
                existing.first == key
            ) {
                return@withLock existing.second to false
            }

            val newDeferred =
                CompletableDeferred<TagsResult>()
            inFlight = key to newDeferred
            newDeferred to true
        }

        if (isOwner) {
            try {
                val result = statsRepository.fetchTags(
                    siteId = siteId,
                    max = max
                )
                mutex.withLock {
                    if (result is TagsResult.Success) {
                        cachedTags =
                            Triple(siteId, max, result.data)
                    }
                    inFlight = null
                }
                deferred.complete(result)
            } catch (e: CancellationException) {
                mutex.withLock { inFlight = null }
                deferred.cancel(e)
                throw e
            } catch (e: Exception) {
                mutex.withLock { inFlight = null }
                deferred.complete(
                    TagsResult.Error(
                        e.message ?: "Unknown error"
                    )
                )
                throw e
            }
        }

        return deferred.await()
    }

    suspend fun clearCache() {
        mutex.withLock {
            cachedTags = null
            inFlight = null
        }
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
