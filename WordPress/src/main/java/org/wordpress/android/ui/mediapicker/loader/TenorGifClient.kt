package org.wordpress.android.ui.mediapicker.loader

import android.content.Context
import com.tenor.android.core.constant.AspectRatioRange
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.network.ApiClient
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.impl.GifsResponse
import org.wordpress.android.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class TenorGifClient @Inject constructor(private val context: Context, private val tenorApiClient: IApiClient) {
    fun search(
        query: String,
        position: Int,
        loadSize: Int?,
        onSuccess: (GifsResponse) -> Unit,
        onFailure: (Throwable?) -> Unit
    ) {
        tenorApiClient.search(
            ApiClient.getServiceIds(context),
            query,
            loadSize.fittedToMaximumAllowed,
            position.toString(),
            MediaFilter.BASIC,
            AspectRatioRange.ALL
        ).apply {
            enqueue(object : Callback<GifsResponse> {
                override fun onResponse(call: Call<GifsResponse>, response: Response<GifsResponse>) {
                    val errorMessage = context.getString(R.string.gif_picker_empty_search_list)
                    response.body()?.let(onSuccess) ?: onFailure(GifMediaRequestFailedException(errorMessage))
                }

                override fun onFailure(call: Call<GifsResponse>, throwable: Throwable) {
                    onFailure(throwable)
                }
            })
        }
    }

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
    }
}
