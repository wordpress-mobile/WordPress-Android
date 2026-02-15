package org.wordpress.android.ui.mediapicker.loader

import android.content.Context
import com.tenor.android.core.constant.MediaCollectionFormat
import com.tenor.android.core.model.impl.Result
import org.wordpress.android.R
import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.GifMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Empty
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Success
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UriUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GifMediaDataSource
@Inject constructor(
    private val context: Context,
    private val tenorClient: TenorGifClient,
    private val uriUtilsWrapper: UriUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : MediaSource {
    private var nextPosition: Int = 0
    private val items = mutableListOf<MediaItem>()
    private var lastFilter: String? = null

    override suspend fun load(forced: Boolean, loadMore: Boolean, filter: String?): MediaLoadingResult {
        if (!loadMore) {
            lastFilter = filter
            items.clear()
            nextPosition = 0
        }

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return Failure(
                UiStringRes(R.string.no_network_message),
                data = items
            )
        }

        return if (!filter.isNullOrBlank()) {
            suspendCoroutine { cont ->
                tenorClient.search(filter,
                    nextPosition,
                    PAGE_SIZE,
                    onSuccess = { response ->
                        val gifList = response.results.mapNotNull { it.toMediaItem() }

                        items.addAll(gifList)
                        val newPosition = response.next.toIntOrNull() ?: 0
                        val hasMore = newPosition > nextPosition
                        nextPosition = newPosition
                        val result = if (items.isNotEmpty()) {
                            Success(items.toList(), hasMore)
                        } else {
                            Empty(UiStringRes(R.string.gif_picker_empty_search_list))
                        }
                        cont.resume(result)
                    },
                    onFailure = {
                        val errorMessage = it?.message
                            ?: context.getString(R.string.gif_list_search_returned_unknown_error)
                        cont.resume(
                            Failure(
                                UiStringRes(R.string.media_loading_failed),
                                htmlSubtitle = UiStringText(errorMessage),
                                data = items
                            )
                        )
                    }
                )
            }
        } else {
            buildDefaultScreen()
        }
    }

    private fun buildDefaultScreen(): MediaLoadingResult {
        val title = UiStringRes(R.string.gif_picker_initial_empty_text)
        return Empty(
            title,
            null,
            R.drawable.img_illustration_media_105dp,
            R.drawable.img_tenor_100dp,
            UiStringRes(R.string.gif_powered_by_tenor)
        )
    }

    private fun Result.toMediaItem(): MediaItem? {
        urlFromCollectionFormat(MediaCollectionFormat.GIF)?.let { gifUrl ->
            urlFromCollectionFormat(MediaCollectionFormat.GIF_NANO)?.let { gifNanoUrl ->
                return MediaItem(
                    identifier = GifMediaIdentifier(
                        uriUtilsWrapper.parse(gifUrl),
                        title
                    ),
                    url = uriUtilsWrapper.parse(gifNanoUrl).toString(),
                    type = IMAGE,
                    dataModified = 0
                )
            }
        }
        return null
    }

    private fun Result.urlFromCollectionFormat(format: String) =
        medias.firstOrNull()?.get(format)?.url

    companion object {
        private const val PAGE_SIZE = 36
    }
}
