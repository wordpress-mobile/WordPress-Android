package org.wordpress.android.ui.mediapicker.loader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.StockMediaStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.StockMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Success
import javax.inject.Inject
import javax.inject.Named

class StockMediaDataSource
@Inject constructor(
    private val stockMediaStore: StockMediaStore,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : MediaSource {
    override suspend fun load(
        forced: Boolean,
        loadMore: Boolean,
        filter: String?
    ): MediaLoadingResult {
        return withValidFilter(filter) { validFilter ->
            val result = stockMediaStore.fetchStockMedia(validFilter, loadMore)
            val error = result.error
            return@withValidFilter when {
                error != null -> {
                    Failure(error.message)
                }
                else -> Success(get(), result.canLoadMore)
            }
        } ?: Success(listOf(), false)
    }

    private suspend fun get(): List<MediaItem> {
        return stockMediaStore.getStockMedia()
                .mapNotNull {
                    it.url?.let { url ->
                        MediaItem(
                                StockMediaIdentifier(it.url, it.name, it.title),
                                url,
                                it.name,
                                IMAGE,
                                null,
                                it.date?.toLongOrNull() ?: 0
                        )
                    }
                }
    }

    private suspend fun <T> withValidFilter(filter: String?, action: suspend (filter: String) -> T): T? {
        return if (filter != null && filter.length >= MIN_SEARCH_QUERY_SIZE) {
            withContext(bgDispatcher) {
                action(filter)
            }
        } else {
            null
        }
    }

    companion object {
        private const val MIN_SEARCH_QUERY_SIZE = 3
    }
}
