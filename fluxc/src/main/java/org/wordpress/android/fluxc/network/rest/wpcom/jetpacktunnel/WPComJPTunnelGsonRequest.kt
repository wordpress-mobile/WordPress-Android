package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import java.lang.reflect.Type

/**
 * A request making a WP-API call on a Jetpack site via the WordPress.com <code>/jetpack-blogs/$site/rest-api</code>
 * tunnel.
 */
object WPComJPTunnelGsonRequest {
    private val gson by lazy { Gson() }

    /**
     * Creates a new GET request to the given WP-API endpoint, calling it via the WP.com Jetpack WP-API tunnel.
     *
     * @param wpApiEndpoint the WP-API request endpoint (e.g. /wp/v2/posts/)
     * @param siteId the WordPress.com site ID
     * @param params the parameters to append to the request URL
     * @param type the Type defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     *
     * @param T the expected response object from the WP-API endpoint
     */
    fun <T : Any> buildGetRequest(wpApiEndpoint: String, siteId: Long, params: Map<String, String>,
                                  type: Type, listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val wrappedParams = createTunnelParams(params, wpApiEndpoint)

        val tunnelRequestUrl = getTunnelApiUrl(siteId)
        val wrappedType = TypeToken.getParameterized(JPTunnelWPComRestResponse::class.java, type).type
        val wrappedListener = Response.Listener<JPTunnelWPComRestResponse<T>> { listener(it.data) }

        return WPComGsonRequest.buildGetRequest(tunnelRequestUrl, wrappedParams, wrappedType,
                wrappedListener, errorListener)
    }

    /**
     * Creates a new POST request to the given WP-API endpoint, calling it via the WP.com Jetpack WP-API tunnel.
     *
     * @param wpApiEndpoint the WP-API request endpoint (e.g. /wp/v2/posts/)
     * @param siteId the WordPress.com site ID
     * @param body the request body
     * @param type the Type defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     *
     * @param T the expected response object from the WP-API endpoint
     */
    fun <T : Any> buildPostRequest(wpApiEndpoint: String, siteId: Long, body: Map<String, Any>,
                                   type: Type, listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val wrappedBody = createTunnelBody(method = "post", body = body, path = wpApiEndpoint)
        return buildWrappedPostRequest(siteId, wrappedBody, type, listener, errorListener)
    }

    /**
     * Creates a new PATCH request to the given WP-API endpoint, calling it via the WP.com Jetpack WP-API tunnel.
     *
     * @param wpApiEndpoint the WP-API request endpoint (e.g. /wp/v2/posts/)
     * @param siteId the WordPress.com site ID
     * @param body the request body
     * @param type the Type defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     *
     * @param T the expected response object from the WP-API endpoint
     */
    fun <T : Any> buildPatchRequest(wpApiEndpoint: String, siteId: Long, body: Map<String, Any>,
                                    type: Type, listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val wrappedBody = createTunnelBody(method = "patch", body = body, path = wpApiEndpoint)
        return buildWrappedPostRequest(siteId, wrappedBody, type, listener, errorListener)
    }

    /**
     * Creates a new PUT request to the given WP-API endpoint, calling it via the WP.com Jetpack WP-API tunnel.
     *
     * @param wpApiEndpoint the WP-API request endpoint (e.g. /wp/v2/posts/)
     * @param siteId the WordPress.com site ID
     * @param body the request body
     * @param type the Type defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     *
     * @param T the expected response object from the WP-API endpoint
     */
    fun <T : Any> buildPutRequest(wpApiEndpoint: String, siteId: Long, body: Map<String, Any>,
                                  type: Type, listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val wrappedBody = createTunnelBody(method = "put", body = body, path = wpApiEndpoint)
        return buildWrappedPostRequest(siteId, wrappedBody, type, listener, errorListener)
    }

    /**
     * Creates a new DELETE request to the given WP-API endpoint, calling it via the WP.com Jetpack WP-API tunnel.
     *
     * @param wpApiEndpoint the WP-API request endpoint (e.g. /wp/v2/posts/)
     * @param siteId the WordPress.com site ID
     * @param params the parameters to append to the request URL
     * @param type the Type defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     *
     * @param T the expected response object from the WP-API endpoint
     */
    fun <T : Any> buildDeleteRequest(wpApiEndpoint: String, siteId: Long, params: Map<String, String>,
                                     type: Type, listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val wrappedBody = createTunnelBody(method = "delete", params = params, path = wpApiEndpoint)
        return buildWrappedPostRequest(siteId, wrappedBody, type, listener, errorListener)
    }

    private fun <T : Any> buildWrappedPostRequest(siteId: Long, wrappedBody: Map<String, Any>, type: Type,
                                                  listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val tunnelRequestUrl = getTunnelApiUrl(siteId)
        val wrappedType = TypeToken.getParameterized(JPTunnelWPComRestResponse::class.java, type).type
        val wrappedListener = Response.Listener<JPTunnelWPComRestResponse<T>> { listener(it.data) }

        return WPComGsonRequest.buildPostRequest(tunnelRequestUrl, wrappedBody, wrappedType,
                wrappedListener, errorListener)
    }

    private fun getTunnelApiUrl(siteId: Long): String = WPCOMREST.jetpack_blogs.site(siteId).rest_api.urlV1_1

    private fun createTunnelParams(params: Map<String, String>, path: String): MutableMap<String, String> {
        val finalParams = mutableMapOf<String, String>()
        with(finalParams) {
            put("path", buildRestApiPath(path, params, "get"))
            put("json", "true")
        }
        return finalParams
    }

    private fun createTunnelBody(method: String, body: Map<String, Any> = mapOf(),
                                 params: Map<String, String> = mapOf(), path: String): MutableMap<String, Any> {
        val finalBody = mutableMapOf<String, Any>()
        with(finalBody) {
            put("path", buildRestApiPath(path, params, method))
            put("json", "true")
            if (body.isNotEmpty()) {
                put("body", gson.toJson(body))
            }
        }
        return finalBody
    }

    private fun buildRestApiPath(path: String, params: Map<String, String>, method: String): String {
        var result = path + "&_method=" + method
        if (params.isNotEmpty()) {
            for (param in params) {
                result += "&" + param.key + "=" + param.value
            }
        }
        return result
    }
}
