package org.wordpress.android.ui.media

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaListParams
import uniffi.wp_api.MediaRequestListWithEditContextResponse
import uniffi.wp_api.PostListParams
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

            media.successfulResponse()?.data?.forEach {
                Log.d("MEDIA_TAG", "Media: ${it.mediaType}")
            } ?: run { Log.d("MEDIA_TAG", "No media") }
        }
    }
}
