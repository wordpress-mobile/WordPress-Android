package org.wordpress.android.ui.gifs.provider

import android.content.Context
import com.tenor.android.core.constant.AspectRatioRange
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.network.ApiClient
import com.tenor.android.core.network.ApiService
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.WeakRefCallback
import com.tenor.android.core.response.impl.GifsResponse
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.gifs.provider.GifProvider.Gif

internal class TenorProvider(
    val context: Context,
    client: IApiClient? = null
) : GifProvider {
    private val tenorClient: IApiClient
    private val searchResultLimit = 10

    init {
        ApiService.Builder(context, IApiClient::class.java).apply {
            apiKey(BuildConfig.TENOR_API_KEY)
            ApiClient.init(context, this)
            tenorClient = client ?: ApiClient.getInstance(context)
        }
    }

    override fun search(
        query: String,
        onSuccess: (List<Gif>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        tenorClient.simpleSearch(query,
                onSuccess = { response ->
                    response?.run { results.map { Gif(it.url) } }
                            ?.let { onSuccess(it) }
                            ?: onFailure(context.getString(GifProvider.unknownErrorStringId))
                },
                onFail = {
                    onFailure(context.getString(GifProvider.queryReturnedNothingStringId))
                }
        )
    }

    private inline fun IApiClient.simpleSearch(
        query: String,
        crossinline onSuccess: (GifsResponse?) -> Unit,
        crossinline onFail: (Throwable?) -> Unit
    ) {
        search(
                ApiClient.getServiceIds(context),
                query,
                searchResultLimit,
                "",
                MediaFilter.BASIC,
                AspectRatioRange.ALL

        ).enqueue(object : WeakRefCallback<Context, GifsResponse>(context) {
            override fun success(ctx: Context, response: GifsResponse?) {
                onSuccess(response)
            }

            override fun failure(ctx: Context, throwable: Throwable?) {
                onFail(throwable)
            }
        })
    }
}
