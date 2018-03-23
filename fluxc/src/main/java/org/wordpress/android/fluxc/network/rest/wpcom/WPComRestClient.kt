package org.wordpress.android.fluxc.network.rest.wpcom

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject

class WPComRestClient
@Inject constructor(appContext: Context,
                    dispatcher: Dispatcher,
                    requestQueue: RequestQueue,
                    accessToken: AccessToken,
                    userAgent: UserAgent) :
        BaseWPComRestClient(appContext,
                dispatcher,
                requestQueue,
                accessToken,
                userAgent) {
    fun enqueueRequest(request: WPComGsonRequest<*>): Request<*> {
        return add(request, true)
    }
}
