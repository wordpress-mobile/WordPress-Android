package org.wordpress.android.fluxc.network.rest.wpapi

import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError

class WPAPINetworkError(
    baseError: BaseNetworkError,
    val errorCode: String? = null
) : BaseNetworkError(baseError)
