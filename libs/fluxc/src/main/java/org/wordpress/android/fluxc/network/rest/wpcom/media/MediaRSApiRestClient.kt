package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.MimeType
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.MediaRequestListWithEditContextResponse
import uniffi.wp_api.MediaWithEditContext
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * MediaRSApiRestClient provides an interface for calling media endpoints using the WordPress Rust library
 */
@Singleton
class MediaRSApiRestClient @Inject constructor() {
    private val applicationScope by lazy { CoroutineScope(Dispatchers.IO) }

    fun fetchMediaList(site: SiteModel, number: Int, offset: Int, mimeType: MimeType.Type?) {
        applicationScope.launch {
            val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
            )
            val apiRootUrl = URL("${site.url}/wp-json")
            val client = WpApiClient(
                wpOrgSiteApiRootUrl = apiRootUrl,
                authProvider = authProvider,
                appNotifier = object : WpAppNotifier {
                    override suspend fun requestedWithInvalidAuthentication() {
                        Log.d("MEDIA_TAG", "NOT AUTHENTICATED")
                    }
                }
            )
            val media: WpRequestResult<MediaRequestListWithEditContextResponse> = client.request { requestBuilder ->
                requestBuilder.media().listWithEditContext(MediaListParams())
            }

            val mediaModelList = media.successfulResponse()?.data?.toMediaModelList(site.id)
            mediaModelList?.forEach {
                Log.d("MEDIA_TAG", it.url)
            } ?: run {
                Log.d("MEDIA_TAG", "MEDIA MODEL LIST IS NULL")
            }
        }
    }

    private fun List<MediaWithEditContext>.toMediaModelList(
        siteId: Int
    ): List<MediaModel> = map { it.toMediaModel(siteId) }

    private fun MediaWithEditContext.toMediaModel(
        siteId: Int
    ): MediaModel = MediaModel(siteId, id).apply {
        // Map URLs
        url = this@toMediaModel.sourceUrl
        guid = this@toMediaModel.link

        // Map file information
        title = this@toMediaModel.title.rendered
        caption = this@toMediaModel.caption.rendered
        description = this@toMediaModel.description.rendered
        alt = this@toMediaModel.altText

        // Map media type and mime type
        mimeType = this@toMediaModel.mimeType
        fileExtension = this@toMediaModel.mediaType.toString()

        // Map media details if available
        this@toMediaModel.mediaDetails.let { details ->
            // The exact structure of MediaDetails depends on the uniffi generated code
            // You may need to adjust these based on the actual properties available
        }

        // Map dates
        uploadDate = this@toMediaModel.date

        // Map author
        authorId = this@toMediaModel.author

        // Set upload state as uploaded since this is fetched from server
        uploadState = org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED.toString()
    }
}
