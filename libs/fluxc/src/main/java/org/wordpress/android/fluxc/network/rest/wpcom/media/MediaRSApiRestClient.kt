package org.wordpress.android.fluxc.network.rest.wpcom.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaDetailsPayload
import uniffi.wp_api.MediaListParams
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
class MediaRSApiRestClient @Inject constructor(
    @Named(FLUXC_SCOPE) private val scope: CoroutineScope,
    private val dispatcher: Dispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val wpAppNotifierHandler: WpAppNotifierHandler,
) {
    fun fetchMediaList(site: SiteModel, number: Int, offset: Int, mimeType: MimeType.Type?) {
        scope.launch {
            val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
            )
            val apiRootUrl = URL(site.buildUrl())
            val client = WpApiClient(
                wpOrgSiteApiRootUrl = apiRootUrl,
                authProvider = authProvider,
                appNotifier = object : WpAppNotifier {
                    override suspend fun requestedWithInvalidAuthentication() {
                        wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                    }
                }
            )
            val mediaResponse = client.request { requestBuilder ->
                requestBuilder.media().listWithEditContext(
                    MediaListParams(
                        perPage = number.toUInt(),
                        offset = offset.toUInt(),
                        mimeType = mimeType?.name
                    )
                )
            }


            val mediaModelList = when (mediaResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.MAIN, "Fetched media list: ${mediaResponse.response.data.size}")
                    mediaResponse.response.data.toMediaModelList(site.id)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.MAIN, "Fetch media list failed: $mediaResponse")
                    emptyList()
                }
            }
            val canLoadMore = mediaModelList.size == number
            notifyMediaListFetched(site, mediaModelList, offset > 0, canLoadMore, mimeType)
        }
    }

    private fun SiteModel.buildUrl(): String = wpApiRestUrl ?: "${url}/wp-json"

    private fun notifyMediaListFetched(
        site: SiteModel,
        media: List<MediaModel>,
        loadedMore: Boolean,
        canLoadMore: Boolean,
        mimeType: MimeType.Type?
    ) {
        val payload = FetchMediaListResponsePayload(
            site, media,
            loadedMore, canLoadMore, mimeType
        )
        dispatcher.dispatch(MediaActionBuilder.newFetchedMediaListAction(payload))
    }

    private fun List<MediaWithEditContext>.toMediaModelList(
        siteId: Int
    ): List<MediaModel> = map { it.toMediaModel(siteId) }

    private fun MediaWithEditContext.toMediaModel(
        siteId: Int
    ): MediaModel = MediaModel(siteId, id).apply {
        url = this@toMediaModel.sourceUrl
        guid = this@toMediaModel.link
        title = this@toMediaModel.title.rendered
        caption = this@toMediaModel.caption.rendered
        description = this@toMediaModel.description.rendered
        alt = this@toMediaModel.altText
        postId = this@toMediaModel.postId ?: 0
        mimeType = this@toMediaModel.mimeType
        fileExtension = this@toMediaModel.mediaType.toString()
        uploadDate = this@toMediaModel.date
        authorId = this@toMediaModel.author
        uploadState = org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED.toString()

        // Parse the media details
        when (val parsedType = this@toMediaModel.mediaDetails.parseAsMimeType(this@toMediaModel.mimeType)) {
            is MediaDetailsPayload.Audio -> length = parsedType.v1.length.toInt()
            is MediaDetailsPayload.Image -> {
                width = parsedType.v1.width.toInt()
                height = parsedType.v1.height.toInt()
                thumbnailUrl = parsedType.v1.sizes?.get("thumbnail")?.sourceUrl
                fileUrlMediumSize = parsedType.v1.sizes?.get("medium")?.sourceUrl
                fileUrlLargeSize = parsedType.v1.sizes?.get("large")?.sourceUrl
            }
            is MediaDetailsPayload.Video -> {
                width = parsedType.v1.width.toInt()
                height = parsedType.v1.height.toInt()
                length = parsedType.v1.length.toInt()
            }
            is MediaDetailsPayload.Document,
            null -> {}
        }
    }
}
