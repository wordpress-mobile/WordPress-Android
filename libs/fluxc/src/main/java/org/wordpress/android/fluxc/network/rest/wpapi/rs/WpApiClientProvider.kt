package org.wordpress.android.fluxc.network.rest.wpapi.rs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider.MockedRequestExecutor.UploadListener
import org.wordpress.android.fluxc.network.rest.wpcom.media.ProgressRequestBody
import rs.wordpress.api.kotlin.DefaultFileResolver
import rs.wordpress.api.kotlin.FileResolver
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpHttpClient
import rs.wordpress.api.kotlin.WpRequestExecutor
import uniffi.wp_api.MediaUploadRequest
import uniffi.wp_api.MediaUploadRequestExecutionException
import uniffi.wp_api.RequestExecutor
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import uniffi.wp_api.WpNetworkHeaderMap
import uniffi.wp_api.WpNetworkRequest
import uniffi.wp_api.WpNetworkResponse
import java.io.File
import java.net.URL
import javax.inject.Inject

class WpApiClientProvider @Inject constructor(
    private val wpAppNotifierHandler: WpAppNotifierHandler,
) {
    fun getWpApiClient(
        site: SiteModel,
        uploadListener: UploadListener? = null
    ): WpApiClient {
        val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
        )
        val apiRootUrl = URL(site.buildUrl())
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = apiRootUrl,
            authProvider = authProvider,
            requestExecutor = MockedRequestExecutor(uploadListener = uploadListener),
            appNotifier = object : WpAppNotifier {
                override suspend fun requestedWithInvalidAuthentication() {
                    wpAppNotifierHandler.notifyRequestedWithInvalidAuthentication(site)
                }
            }
        )
        return client
    }

    private fun SiteModel.buildUrl(): String = wpApiRestUrl ?: "${url}/wp-json"

    class MockedRequestExecutor(
        private val httpClient: WpHttpClient = WpHttpClient.DefaultHttpClient(),
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
        private val fileResolver: FileResolver = DefaultFileResolver(),
        private val uploadListener: UploadListener? = null
    ) : RequestExecutor {
        private val wpRequestExecutor = WpRequestExecutor()
        override suspend fun execute(request: WpNetworkRequest) = wpRequestExecutor.execute(request)

        override suspend fun sleep(millis: ULong) {
            wpRequestExecutor.sleep(millis)
        }

        override suspend fun uploadMedia(mediaUploadRequest: MediaUploadRequest): WpNetworkResponse =
            withContext(dispatcher) {
                val requestBuilder = Request.Builder().url(mediaUploadRequest.url())
                val multipartBodyBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                mediaUploadRequest.mediaParams().forEach { (k, v) ->
                    multipartBodyBuilder.addFormDataPart(k, v)
                }
                val file = fileResolver.getFile(mediaUploadRequest.filePath())
                if (file == null || !file.canBeUploaded()) {
                    throw MediaUploadRequestExecutionException.MediaFileNotFound(mediaUploadRequest.filePath())
                }
                val progressRequestBody = getRequestBody(file, mediaUploadRequest, uploadListener)
                multipartBodyBuilder.addFormDataPart(
                    name = "file",
                    filename = file.name,
                    body = progressRequestBody
                )
                requestBuilder.method(
                    method = mediaUploadRequest.method().toString(),
                    body = multipartBodyBuilder.build()
                )
                mediaUploadRequest.headerMap().toMap().forEach { (key, values) ->
                    values.forEach { value ->
                        requestBuilder.addHeader(key, value)
                    }
                }

                val call = httpClient.getClient().newCall(requestBuilder.build())
                // Notify about the call creation so it can be cancelled if needed
                uploadListener?.onUploadStarted(call)
                call.execute().use { response ->
                    return@withContext WpNetworkResponse(
                        body = response.body?.bytes() ?: ByteArray(0),
                        statusCode = response.code.toUShort(),
                        responseHeaderMap = WpNetworkHeaderMap.fromMultiMap(response.headers.toMultimap()),
                        requestUrl = mediaUploadRequest.url(),
                        requestHeaderMap = mediaUploadRequest.headerMap()
                    )
                }
            }

        private fun getRequestBody(
            file: File,
            mediaUploadRequest: MediaUploadRequest,
            uploadListener: UploadListener?
        ): RequestBody {
            val fileRequestBody = file.asRequestBody(mediaUploadRequest.fileContentType().toMediaType())
            return if (uploadListener != null) {
                ProgressRequestBody(
                    delegate = fileRequestBody,
                    progressListener = object : ProgressRequestBody.ProgressListener {
                        override fun onProgress(bytesWritten: Long, contentLength: Long) {
                            uploadListener.onProgressUpdate(bytesWritten, contentLength)
                        }
                    }
                )
            } else {
                fileRequestBody
            }
        }

        private fun File.canBeUploaded() = exists() && isFile && canRead()

        interface UploadListener {
            fun onProgressUpdate(uploadedBytes: Long, totalBytes: Long)
            fun onUploadStarted(uploadCall: Call)
        }
    }
}
