package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

fun interface OnWPAPIErrorListener {
    fun onErrorResponse(error: WPAPINetworkError)
}

class WPAPIErrorListenerWrapper(
    private val listener: OnWPAPIErrorListener
) : BaseRequest.BaseErrorListener {
    override fun onErrorResponse(error: BaseNetworkError) {
        listener.onErrorResponse(error as WPAPINetworkError)
    }
}
