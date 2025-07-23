package org.wordpress.android.fluxc.network.rest.wpapi.rs

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WpAppNotifierHandler
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
    fun getWpApiClient(site: SiteModel): WpApiClient {
        val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
        )
        val apiRootUrl = URL(site.buildUrl())
        val client = WpApiClient(
            wpOrgSiteApiRootUrl = apiRootUrl,
            authProvider = authProvider,
            requestExecutor = MockedRequestExecutor(),
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
        private val fileResolver: FileResolver = DefaultFileResolver()
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
                multipartBodyBuilder.addFormDataPart(
                    name = "file",
                    filename = file.name,
                    body = file.asRequestBody(mediaUploadRequest.fileContentType().toMediaType())
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

                httpClient.getClient().newCall(requestBuilder.build()).execute().use { response ->
                    return@withContext WpNetworkResponse(
                        body = response.body?.bytes() ?: ByteArray(0),
                        statusCode = response.code.toUShort(),
                        responseHeaderMap = WpNetworkHeaderMap.fromMultiMap(response.headers.toMultimap()),
                        requestUrl = mediaUploadRequest.url(),
                        requestHeaderMap = mediaUploadRequest.headerMap()
                    )
                }
            }

        private fun File.canBeUploaded() = exists() && isFile && canRead()
    }
}
