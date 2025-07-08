package org.wordpress.android.ui.media

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.MediaRequestListWithEditContextResponse
import uniffi.wp_api.MediaWithEditContext
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

class MediaGridViewModel @Inject constructor() : ViewModel() {
    fun fetchMediaList(site: SiteModel) {
        viewModelScope.launch {
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

            media.successfulResponse()?.data?.forEach { mediaWithEditContext ->
                val mediaModel = mapMediaWithEditContextToMediaModel(mediaWithEditContext, site.id)
                Log.d("MEDIA_TAG", "Media: ${mediaModel.url}")
                // Use mediaModel here
            } ?: run { Log.d("MEDIA_TAG", "No media") }
        }
    }

    private fun mapMediaWithEditContextToMediaModel(
        mediaWithEditContext: MediaWithEditContext,
        siteId: Int
    ): MediaModel = MediaModel(siteId, mediaWithEditContext.id).apply {
        // Map URLs
        url = mediaWithEditContext.sourceUrl
        guid = mediaWithEditContext.link

        // Map file information
        title = mediaWithEditContext.title.rendered
        caption = mediaWithEditContext.caption.rendered
        description = mediaWithEditContext.description.rendered
        alt = mediaWithEditContext.altText

        // Map media type and mime type
        mimeType = mediaWithEditContext.mimeType
        fileExtension = mediaWithEditContext.mediaType.toString()

        // Map media details if available
        mediaWithEditContext.mediaDetails.let { details ->
            // The exact structure of MediaDetails depends on the uniffi generated code
            // You may need to adjust these based on the actual properties available
        }

        // Map dates
        uploadDate = mediaWithEditContext.date

        // Map author
        authorId = mediaWithEditContext.author

        // Set upload state as uploaded since this is fetched from server
        uploadState = MediaModel.MediaUploadState.UPLOADED.toString()
    }
}
