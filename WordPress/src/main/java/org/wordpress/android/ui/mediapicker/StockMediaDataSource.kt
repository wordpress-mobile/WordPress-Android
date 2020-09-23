package org.wordpress.android.ui.mediapicker

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StockMediaStore
import org.wordpress.android.fluxc.store.StockMediaUploadItem
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.StockMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaSource.MediaInsertResult
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult.NoChange
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult.Success
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import javax.inject.Inject
import javax.inject.Named

class StockMediaDataSource(
    private val site: SiteModel,
    private val stockMediaStore: StockMediaStore,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : MediaSource {
    override suspend fun load(
        forced: Boolean,
        loadMore: Boolean,
        filter: String?
    ): MediaLoadingResult {
        return withValidFilter(filter) { validFilter ->
            if (loadMore || forced) {
                val result = stockMediaStore.fetchStockMedia(validFilter, loadMore)
                val error = result.error
                return@withValidFilter when {
                    error != null -> {
                        Failure(error.message)
                    }
                    else -> Success(get(validFilter), result.canLoadMore)
                }
            }
            NoChange
        } ?: NoChange
    }

    private suspend fun get(filter: String?): List<MediaItem> {
        return withValidFilter(filter) { validFilter ->
            stockMediaStore.getStockMedia(validFilter)
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
        } ?: listOf()
    }

    override suspend fun insert(identifiers: List<Identifier>): MediaInsertResult {
        val result = stockMediaStore.performUploadStockMedia(site, identifiers.mapNotNull { identifier ->
            (identifier as? StockMediaIdentifier)?.let {
                StockMediaUploadItem(it.name, it.title, it.url)
            }
        })
        return when {
            result.error != null -> MediaInsertResult.Failure(result.error.message)
            else -> MediaInsertResult.Success(result.mediaList.mapNotNull { Identifier.RemoteId(it.mediaId) })
        }
    }

    private suspend fun <T> withValidFilter(filter: String?, action: suspend (filter: String) -> T): T? {
        return filter?.let {
            if (it.length >= MIN_SEARCH_QUERY_SIZE) {
                withContext(bgDispatcher) {
                    return@withContext action(it)
                }
            }
            return null
        }
    }

    companion object {
        private const val MIN_SEARCH_QUERY_SIZE = 3
    }

    class StockMediaDataSourceFactory
    @Inject constructor(
        val stockMediaStore: StockMediaStore,
        @param:Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher
    ) {
        fun build(site: SiteModel): StockMediaDataSource {
            return StockMediaDataSource(site, stockMediaStore, bgDispatcher)
        }
    }
}
