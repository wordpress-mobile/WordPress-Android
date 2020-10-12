package org.wordpress.android.ui.mediapicker.loader

import android.content.Context
import android.net.Uri
import com.tenor.android.core.constant.AspectRatioRange
import com.tenor.android.core.constant.MediaCollectionFormat
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.model.impl.Result
import com.tenor.android.core.network.ApiClient
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.impl.GifsResponse
import org.wordpress.android.R

import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.mediapicker.MediaItem.Identifier.GifMediaIdentifier
import org.wordpress.android.ui.mediapicker.MediaType.IMAGE
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Success
import org.wordpress.android.viewmodel.gif.provider.GifProvider.GifRequestFailedException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GifMediaDataSource
@Inject constructor(
    private val context: Context,
    private val tenorClient: IApiClient
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

        return if (!filter.isNullOrBlank()) {
            suspendCoroutine<MediaLoadingResult> { cont ->
                search(filter,
                        nextPosition,
                        PAGE_SIZE,
                        onSuccess = { response ->
                            val gifList = response.results.map { it.toMediaItem() }

                            items.addAll(gifList)
                            val newPosition = response.next.toIntOrNull() ?: 0
                            val hasMore = newPosition > nextPosition
                            nextPosition = newPosition

                            cont.resume(Success(items.toList(), hasMore))
                        },
                        onFailure = {
                            val errorMessage = it?.message
                                    ?: context.getString(R.string.gif_list_search_returned_unknown_error)
                            cont.resume(Failure(errorMessage))
                        }
                )
            }
        } else {
            Success(listOf(), false)
        }
    }

    private inline fun search(
        query: String,
        position: Int,
        loadSize: Int?,
        crossinline onSuccess: (GifsResponse) -> Unit,
        crossinline onFailure: (Throwable?) -> Unit
    ) {
        tenorClient.search(ApiClient.getServiceIds(context),
                query,
                loadSize.fittedToMaximumAllowed,
                position.toString(),
                MediaFilter.BASIC,
                AspectRatioRange.ALL).apply {
            enqueue(object : Callback<GifsResponse> {
                override fun onResponse(call: Call<GifsResponse>, response: Response<GifsResponse>) {
                    val errorMessage = context.getString(R.string.gif_picker_empty_search_list)
                    response.body()?.let(onSuccess) ?: onFailure(GifRequestFailedException(errorMessage))
                }

                override fun onFailure(call: Call<GifsResponse>, throwable: Throwable) {
                    onFailure(throwable)
                }
            })
        }
    }

    private fun Result.toMediaItem() = MediaItem(
            identifier = GifMediaIdentifier(
            id,
            null,
            Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF_NANO)),
            Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF_TINY)),
            Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF)),
            title),
            url = Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF_NANO)).toString(),
            type = IMAGE,
            dataModified = 0
    )

    private fun Result.urlFromCollectionFormat(format: String) =
            medias.firstOrNull()?.get(format)?.url

    /**
     * Since the Tenor only allows a maximum of 50 GIFs per request, the API will throw
     * an exception if this rule is disrespected, in order to still be resilient in
     * provide the desired search, the loadSize will be reduced to the maximum allowed
     * if needed
     */
    private val Int?.fittedToMaximumAllowed
        get() = this?.let {
            when {
                this > MAXIMUM_ALLOWED_LOAD_SIZE -> MAXIMUM_ALLOWED_LOAD_SIZE
                else -> this
            }
        } ?: MAXIMUM_ALLOWED_LOAD_SIZE

    companion object {
        private const val MAXIMUM_ALLOWED_LOAD_SIZE = 50
        private const val PAGE_SIZE = 36
    }
}
