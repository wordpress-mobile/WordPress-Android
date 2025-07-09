package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MediaDetails
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
                        Log.d("MEDIA_TAG", "NOT AUTHENTICATED")
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

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
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

        // Map media details if available
        this@toMediaModel.mediaDetails.let { details ->
            try {
                val detailsClass: Class<out MediaDetails> = details::class.java

                detailsClass.getDeclaredField("width").let { field ->
                    field.isAccessible = true
                    (field.get(details) as? Number)?.let { width = it.toInt() }
                }

                detailsClass.getDeclaredField("height").let { field ->
                    field.isAccessible = true
                    (field.get(details) as? Number)?.let { height = it.toInt() }
                }

                detailsClass.getDeclaredField("file").let { field ->
                    field.isAccessible = true
                    (field.get(details) as? String)?.let { fileName = it }
                }

                mapMediaDetails(detailsClass, details)
            } catch (e: Exception) {
                // Ignore reflection errors - fields may not exist in this version
                appLogWrapper.d(AppLog.T.MEDIA, "Could not access MediaDetails fields: ${e.message}")
            }
        }
    }

    private fun MediaModel.mapMediaDetails(
        detailsClass: Class<out MediaDetails>,
        details: MediaDetails
    ) {
        detailsClass.getDeclaredField("sizes").let { sizesField ->
            sizesField.isAccessible = true
            val sizes = sizesField.get(details)

            if (sizes != null) {
                val sizesClass = sizes::class.java
                fileUrlMediumSize = getSizeField("medium", sizesClass, sizes)
                fileUrlLargeSize = getSizeField("large", sizesClass, sizes)
                thumbnailUrl = getSizeField("thumbnail", sizesClass, sizes)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
    private fun getSizeField(
        name: String,
        sizesClass: Class<out Any>,
        sizes: Any?
    ): String? = try {
            sizesClass.getDeclaredField(name).let { field ->
                field.isAccessible = true
                val internalField = field.get(sizes)
                if (internalField != null) {
                    val thumbnailClass = internalField::class.java
                    thumbnailClass.getDeclaredField("sourceUrl").let { sourceField ->
                        sourceField.isAccessible = true
                        (sourceField.get(internalField) as? String)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
}
