package org.wordpress.android.fluxc.network.rest.wpcom.whatsnew

import android.content.Context
import com.android.volley.RequestQueue
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient.WhatsNewResponse.Announce
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewFetchedPayload
import javax.inject.Singleton

@Singleton
class WhatsNewRestClient constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    private val firstAnnouncement = WhatsNewAnnouncementModel(
            "15.0",
            1,
            850,
            "https://wordpress.org",
            true,
            "it",
            listOf(
                    WhatsNewAnnouncementFeature(
                            "first announcement feature 1",
                            "first announcement subtitle 1",
                            "",
                            "https://wordpress.org/icon1.png"
                    ),
                    WhatsNewAnnouncementFeature(
                            "first announcement feature 2",
                            "first announcement subtitle 2",
                            "<image data>",
                            ""
                    )
            )
    )

    private val secondAnnouncement = WhatsNewAnnouncementModel(
            "16.0",
            2,
            855,
            "https://wordpress.org/announcement2/",
            false,
            "en",
            listOf(
                    WhatsNewAnnouncementFeature(
                            "second announcement feature 1",
                            "second announcement subtitle 1",
                            "",
                            "https://wordpress.org/icon2.png"
                    ),
                    WhatsNewAnnouncementFeature(
                            "second announcement feature 2",
                            "first announcement subtitle 2",
                            "<second image data>",
                            ""
                    )
            )
    )

    private val testAnnouncements = listOf(firstAnnouncement, secondAnnouncement)

    suspend fun fetchWhatsNew(versionCode: String): WhatsNewFetchedPayload {
        delay(3000)
        return WhatsNewFetchedPayload(testAnnouncements)
//        val url = WPCOMV2.whats_new.mobile.url
//
//        val params = mapOf(
//                "client" to "android",
//                "version" to versionCode
//        )
//
//        val response = wpComGsonRequestBuilder.syncGetRequest(
//                this,
//                url,
//                params,
//                WhatsNewResponse::class.java,
//                enableCaching = false,
//                forced = true
//        )
//
//
//        return when (response) {
//            is Success -> {
////                val announcements = response.data.announcements
////                buildWhatsNewPayload(announcements)
//                return WhatsNewFetchedPayload(testAnnouncements)
//            }
//            is WPComGsonRequestBuilder.Response.Error -> {
//                val payload = WhatsNewFetchedPayload()
//                payload.error = response.error
//                payload
//            }
//        }
    }

    private fun buildWhatsNewPayload(
        announcements: List<Announce>?
    ): WhatsNewFetchedPayload {
        return WhatsNewFetchedPayload(announcements?.map { announce ->
            WhatsNewAnnouncementModel(
                    appVersionName = announce.appVersionName,
                    announcementVersion = announce.announcementVersion,
                    minimumAppVersionCode = announce.minimumAppVersionCode,
                    detailsUrl = announce.detailsUrl,
                    isLocalized = announce.isLocalized,
                    responseLocale = announce.responseLocale,
                    features = announce.features.map {
                        WhatsNewAnnouncementFeature(
                                title = it.title,
                                subtitle = it.subtitle,
                                iconBase64 = it.iconBase64,
                                iconUrl = it.iconUrl
                        )
                    }
            )
        })
    }

    data class WhatsNewResponse(
        val announcements: List<Announce>?
    ) : Response {
        data class Announce(
            val appVersionName: String,
            val announcementVersion: Int,
            val minimumAppVersionCode: Int,
            val detailsUrl: String,
            val isLocalized: Boolean,
            val responseLocale: String,
            val features: List<Feature>
        )

        data class Feature(
            val title: String,
            val subtitle: String,
            val iconBase64: String,
            val iconUrl: String
        )
    }
}
