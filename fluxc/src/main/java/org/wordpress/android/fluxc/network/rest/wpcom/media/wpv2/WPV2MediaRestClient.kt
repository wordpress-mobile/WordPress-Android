package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import android.content.Context
import com.android.volley.RequestQueue
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WPV2MediaRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val coroutineEngine: CoroutineEngine,
    private val okHttpClient: OkHttpClient,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    fun uploadMedia(site: SiteModel, media: MediaModel) {
        coroutineEngine.launch(T.MEDIA, this, "Upload Media using WPCom's /wp/v2 API") {
            syncUploadMedia(site, media)
        }
    }

    suspend fun syncUploadMedia(site: SiteModel, media: MediaModel) {

    }
}
