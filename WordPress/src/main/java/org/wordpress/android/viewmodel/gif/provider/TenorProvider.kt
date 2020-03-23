package org.wordpress.android.viewmodel.gif.provider

import android.content.Context
import android.net.Uri
import com.tenor.android.core.constant.AspectRatioRange
import com.tenor.android.core.constant.MediaCollectionFormat
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.model.impl.MediaCollection
import com.tenor.android.core.model.impl.Result
import com.tenor.android.core.network.ApiClient
import com.tenor.android.core.network.ApiService
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.WeakRefCallback
import com.tenor.android.core.response.impl.GifsResponse
import org.wordpress.android.BuildConfig
import org.wordpress.android.viewmodel.gif.GifMediaViewModel
import org.wordpress.android.viewmodel.gif.MutableGifMediaViewModel
import org.wordpress.android.viewmodel.gif.provider.GifProvider.Gif

/**
 * Implementation of a GifProvider using the Tenor gif API as provider
 *
 * This Provider performs requests to the Tenor API using the [tenorClient].
 */

internal class TenorProvider @JvmOverloads constructor(
    val context: Context,
    tenorClient: IApiClient? = null
) : GifProvider {
    private val apiClient: IApiClient
    private val maximumAllowedLoadSize = 50

    /**
     * Initializes the Tenor API client with the environment API key, if no tenorClient is provided via constructor,
     * the init will use the default implementation provided by the Tenor library.
     */
    init {
        ApiService.Builder(context, IApiClient::class.java).apply {
            apiKey(BuildConfig.TENOR_API_KEY)
            ApiClient.init(context, this)
            apiClient = tenorClient ?: ApiClient.getInstance(context)
        }
    }

    override fun search(
        query: String,
        position: Int,
        loadSize: Int?,
        onSuccess: (List<GifMediaViewModel>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        apiClient.simpleSearch(
                query,
                position.toString(),
                loadSize,
                onSuccess = { response ->
                    response?.run { results.map { it.toMutableGifMediaViewModel() } }
                            ?.let { onSuccess(it) }
                            ?: onFailure(RuntimeException())
                },
                onFailure = { it?.let(onFailure) }
        )
    }

    private inline fun IApiClient.simpleSearch(
        query: String,
        position: String,
        loadSize: Int?,
        crossinline onSuccess: (GifsResponse?) -> Unit,
        crossinline onFailure: (Throwable?) -> Unit
    ) {
        search(
                ApiClient.getServiceIds(context),
                query,
                loadSize.fittedToMaximumAllowed,
                position,
                MediaFilter.BASIC,
                AspectRatioRange.ALL

        ).enqueue(object : WeakRefCallback<Context, GifsResponse>(context) {
            override fun success(ctx: Context, response: GifsResponse?) {
                onSuccess(response)
            }

            override fun failure(ctx: Context, throwable: Throwable?) {
                onFailure(throwable)
            }
        })
    }

    private fun Result.toMutableGifMediaViewModel() = MutableGifMediaViewModel(
            id,
            title,
            medias.first().toGif()
    )

    private fun MediaCollection.toGif() = Gif(
            thumbnailUri = Uri.parse(this[MediaCollectionFormat.GIF_NANO].url),
            previewImageUri = Uri.parse(this[MediaCollectionFormat.GIF_TINY].url),
            largeImageUri = Uri.parse(this[MediaCollectionFormat.GIF].url)
    )

    private val Int?.fittedToMaximumAllowed
        get() = this?.let {
            when {
                this > maximumAllowedLoadSize -> maximumAllowedLoadSize
                else -> this
            }
        } ?: maximumAllowedLoadSize
}
