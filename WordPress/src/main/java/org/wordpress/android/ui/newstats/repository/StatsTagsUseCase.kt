package org.wordpress.android.ui.newstats.repository

import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsTagsUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore
) {
    suspend operator fun invoke(
        siteId: Long,
        max: Int = DEFAULT_MAX_ITEMS
    ): TagsResult {
        val token = accountStore.accessToken
        if (token.isNullOrEmpty()) {
            return TagsResult.Error("No access token")
        }
        statsRepository.init(token)
        return statsRepository.fetchTags(
            siteId = siteId,
            max = max
        )
    }

    companion object {
        private const val DEFAULT_MAX_ITEMS = 10
    }
}
