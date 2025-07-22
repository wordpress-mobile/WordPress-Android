package org.wordpress.android.fluxc.network.rest.wpcom.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.MediaUtils
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaCreateParams
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
            val client = getWpApiClient(site)
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
                    appLogWrapper.d(AppLog.T.MEDIA, "Fetched media list: ${mediaResponse.response.data.size}")
                    mediaResponse.response.data.toMediaModelList(site.id)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.MEDIA, "Fetch media list failed: $mediaResponse")
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

    fun fetchMedia(site: SiteModel, media: MediaModel?) {
        if (media == null) {
            val error = MediaError(MediaErrorType.NULL_MEDIA_ARG)
            error.logMessage = "Requested media is null"
            notifyMediaFetched(site, null, error)
            return
        }

        scope.launch {
            val client = getWpApiClient(site)

            val mediaResponse = client.request { requestBuilder ->
                requestBuilder.media().retrieveWithEditContext(media.mediaId)
            }


            when (mediaResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.MEDIA, "Fetched media with ID: " + media.mediaId)

                    val responseMedia: MediaModel = mediaResponse.response.data.toMediaModel(site.id).apply {
                        localSiteId = site.id
                    }
                    notifyMediaFetched(site, responseMedia, null)
                }

                else -> {
                    val mediaError = parseMediaError(mediaResponse)
                    appLogWrapper.e(AppLog.T.MEDIA, "Fetch media failed: ${mediaError.message}")
                    notifyMediaFetched(site, media, mediaError)
                }
            }
        }
    }

    @Suppress("UseCheckOrError") // Allow to throw IllegalStateException
    private fun parseMediaError(mediaResponse: WpRequestResult<*>): MediaError {
        return when (mediaResponse) {
            is WpRequestResult.Success -> {
                throw IllegalStateException("Success media response should not be parsed as an error")
            }
            is WpRequestResult.MediaFileNotFound<*> -> {
                appLogWrapper.e(AppLog.T.MEDIA, "Media file not found: $mediaResponse")
                MediaError(MediaErrorType.NOT_FOUND).apply {
                    message = "Media file not found"
                }
            }

            is WpRequestResult.ResponseParsingError<*> -> {
                appLogWrapper.e(AppLog.T.MEDIA, "Response parsing error: $mediaResponse")
                MediaError(MediaErrorType.PARSE_ERROR).apply {
                    message = "Failed to parse response"
                }
            }

            is WpRequestResult.SiteUrlParsingError<*> -> {
                appLogWrapper.e(AppLog.T.MEDIA, "Site URL parsing error: $mediaResponse")
                MediaError(MediaErrorType.MALFORMED_MEDIA_ARG).apply {
                    message = "Invalid site URL"
                }
            }

            is WpRequestResult.InvalidHttpStatusCode<*>,
            is WpRequestResult.WpError<*>,
            is WpRequestResult.RequestExecutionFailed<*>,
            is WpRequestResult.UnknownError<*> -> {
                appLogWrapper.e(AppLog.T.MEDIA, "Unknown error: $mediaResponse")
                MediaError(MediaErrorType.GENERIC_ERROR).apply {
                    message = "Unknown error occurred"
                }
            }
        }
    }

    private fun notifyMediaFetched(
        site: SiteModel,
        media: MediaModel?,
        error: MediaError?
    ) {
        val payload = MediaPayload(site, media, error)
        dispatcher.dispatch(MediaActionBuilder.newFetchedMediaAction(payload))
    }

    fun deleteMedia(site: SiteModel, media: MediaModel?) {
        if (media == null) {
            val error = MediaError(MediaErrorType.NULL_MEDIA_ARG)
            error.logMessage =  "Media to delete is null"
            notifyMediaDeleted(site, null, error)
            return
        }

        scope.launch {
            val client = getWpApiClient(site)

            val mediaResponse = client.request { requestBuilder ->
                requestBuilder.media().delete(media.mediaId)
            }

            when (mediaResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.MEDIA, "Deleted media with ID: " + media.mediaId)

                    val responseMedia: MediaModel = mediaResponse.response.data.previous.toMediaModel(site.id).apply {
                        localSiteId = site.id
                    }
                    notifyMediaDeleted(site, responseMedia, null)
                }

                else -> {
                    val mediaError = parseMediaError(mediaResponse)
                    appLogWrapper.e(AppLog.T.MEDIA, "Delete media failed: ${mediaError.message}")
                    notifyMediaDeleted(site, media, mediaError)
                }
            }
        }
    }

    private fun notifyMediaDeleted(
        site: SiteModel,
        media: MediaModel?,
        error: MediaError?
    ) {
        val payload = MediaPayload(site, media, error)
        dispatcher.dispatch(MediaActionBuilder.newDeletedMediaAction(payload))
    }

    fun uploadMedia(site: SiteModel, media: MediaModel?) {
        if (media == null || media.id == 0) {
            // we can't have a MediaModel without an ID - otherwise we can't keep track of them.
            val error = MediaError(MediaErrorType.INVALID_ID)
            if (media == null) {
                error.logMessage = "Media object is null on upload"
            } else {
                error.logMessage = "Media ID is 0 on upload"
            }
            notifyMediaUploaded(media, error)
            return
        }

        if (media.filePath == null || !MediaUtils.canReadFile(media.filePath)) {
            val error = MediaError(MediaErrorType.FS_READ_PERMISSION_DENIED)
            error.logMessage = "Can't read file on upload"
            notifyMediaUploaded(media, error)
            return
        }

        scope.launch {
            val client = getWpApiClient(site)

            val mediaResponse = client.request { requestBuilder ->
                requestBuilder.media().create(
                    params = MediaCreateParams(title = media.title),
                    filePath = media.filePath!!, // We have already checked the nullability but it's mutable
                    fileContentType = media.mimeType.orEmpty(),
                    requestId = null
                )
            }

            when (mediaResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.MEDIA, "Uploaded media with ID: " + media.id)

                    val responseMedia: MediaModel = mediaResponse.response.data.toMediaModel(site.id).apply {
                        id = media.id // be sure we are using the same local id when getting the remote response
                        localSiteId = site.id
                    }
                    notifyMediaUploaded(responseMedia, null)
                }

                else -> {
                    val mediaError = parseMediaError(mediaResponse)
                    appLogWrapper.e(AppLog.T.MEDIA, "Upload media failed: ${mediaError.message}")
                    notifyMediaUploaded(media, mediaError)
                }
            }
        }
    }

    private fun notifyMediaUploaded(media: MediaModel?, error: MediaError?) {
        media?.setUploadState(if (error == null) MediaUploadState.UPLOADED else MediaUploadState.FAILED)
        val payload = ProgressPayload(media, 1f, error == null, error)
        dispatcher.dispatch(UploadActionBuilder.newUploadedMediaAction(payload))
    }

    private fun getWpApiClient(site: SiteModel): WpApiClient {
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
        return client
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
        uploadState = MediaUploadState.UPLOADED.toString()

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
