package org.wordpress.android.viewmodel.gifs.provider

import android.content.Context
import android.net.Uri
import com.tenor.android.core.constant.AspectRatioRange
import com.tenor.android.core.constant.MediaCollectionFormat
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.model.impl.Result
import com.tenor.android.core.network.ApiClient
import com.tenor.android.core.network.ApiService
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.WeakRefCallback
import com.tenor.android.core.response.impl.GifsResponse
import org.wordpress.android.BuildConfig
import org.wordpress.android.R.string
import org.wordpress.android.viewmodel.gifs.GifMediaViewModel
import org.wordpress.android.viewmodel.gifs.MutableGifMediaViewModel
import org.wordpress.android.viewmodel.gifs.provider.GifProvider.GifRequestFailedException

/**
 * Implementation of a GifProvider using the Tenor GIF API as provider
 *
 * This Provider performs requests to the Tenor API using the [tenorClient].
 */

internal class TenorProvider @JvmOverloads constructor(
    val context: Context,
    tenorClient: IApiClient? = null
) : GifProvider {
    private val apiClient: IApiClient
    /**
     * To better refers to the Tenor API maximum GIF limit per request
     */
    private val maximumAllowedLoadSize = 50

    /**
     * Initializes the Tenor API client with the environment API key, if no tenorClient is provided via constructor,
     * the init will use the default implementation provided by the Tenor library.
     */
    init {
        ApiService.Builder(context, IApiClient::class.java).apply {
            apiKey(BuildConfig.TENOR_API_KEY)
            ApiClient.init(context, this)
            /**
             * If we call [ApiClient.getInstance] before the [ApiClient.init] the Tenor API
             * will throw an exception for an illegal operation, but to still make possible for the
             * constructor to have the [tenorClient] as a optional parameter, the actual [apiClient]
             * will be decided after everything is initialized
             */
            apiClient = tenorClient ?: ApiClient.getInstance(context)
        }
    }

    /**
     * Implementation of the [GifProvider] search method, it will call the Tenor client search
     * right away with the provided parameters.
     */
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
                onSuccess = {
                    handleResponse(it, onSuccess, onFailure)
                },
                onFailure = {
                    it?.let { throwable ->
                        handleFailure(throwable, onFailure)
                    }
                }
        )
    }

    /**
     * This method act as a simplification to call a search within the Tenor API and the callback
     * creation
     * [MediaFilter] must be BASIC or the returned Media will not have a displayable thumbnail
     * All other provided parameters are set following the Tenor API Documentation
     *
     * Method is inlined for better high-order functions performance
     */
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

    /**
     * When the Tenor API returns the [GifsResponse] this method will try to parse it
     * to a [List] of [GifMediaViewModel], if the parse fails, the [onFailure] will be called
     * with a [GifRequestFailedException] assuming that no valid GIF was found
     *
     * Method is inlined for better high-order functions performance
     */
    private inline fun handleResponse(
        response: GifsResponse?,
        onSuccess: (List<GifMediaViewModel>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        response?.run { results.map { it.toMutableGifMediaViewModel() } }
                ?.let { onSuccess(it) }
                ?: onFailure(
                        GifRequestFailedException(
                                context.getString(string.giphy_picker_empty_search_list)
                        )
                )
    }

    /**
     * If the search request fails an [GifRequestFailedException] will be passed with the API
     * message. If there's no message provided, a generic message will be applied.
     */
    private inline fun handleFailure(
        throwable: Throwable,
        onFailure: (Throwable) -> Unit
    ) {
        throwable.message?.let { message ->
            onFailure(GifRequestFailedException(message))
        } ?: onFailure(
                GifRequestFailedException(
                        context.getString(string.gifs_list_search_returned_unknown_error)
                )
        )
    }

    /**
     * Every GIF returned by the Tenor will be available as [Result], to better interface
     * with our app, it will be converted to [MutableGifMediaViewModel] to avoid any external
     * coupling with the Tenor API
     */
    private fun Result.toMutableGifMediaViewModel() = MutableGifMediaViewModel(
            id,
            Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF_NANO)),
            Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF_TINY)),
            Uri.parse(urlFromCollectionFormat(MediaCollectionFormat.GIF)),
            title
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
                this > maximumAllowedLoadSize -> maximumAllowedLoadSize
                else -> this
            }
        } ?: maximumAllowedLoadSize
}
