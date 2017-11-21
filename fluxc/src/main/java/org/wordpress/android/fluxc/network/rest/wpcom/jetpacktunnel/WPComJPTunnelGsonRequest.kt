package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.Response
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * A request making a WP-API call on a Jetpack site via the WordPress.com <code>/jetpack-blogs/$site/rest-api</code>
 * tunnel.
 */
object WPComJPTunnelGsonRequest {
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
        val tunnelRequestUrl = getTunnelApiUrl(siteId)
        val wrappedType = TypeToken.getParameterized(JPTunnelWPComRestResponse::class.java, type).type
        val wrappedListener = Response.Listener<JPTunnelWPComRestResponse<T>> { listener(it.data) }

        val wrappedParams = createTunnelParams(params, wpApiEndpoint)

        return WPComGsonRequest.buildGetRequest(tunnelRequestUrl, wrappedParams, wrappedType,
                wrappedListener, errorListener)
    }

    /**
     * Creates a new GET request to the given WP-API endpoint, calling it via the WP.com Jetpack WP-API tunnel.
     *
     * @param wpApiEndpoint the WP-API request endpoint (e.g. /wp/v2/posts/)
     * @param siteId the WordPress.com site ID
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     *
     * @param T the expected response object from the WP-API endpoint
     */
    fun <T : Any> buildGetRequest(wpApiEndpoint: String, siteId: Long, params: Map<String, String>,
                                  clazz: KClass<T>, listener: (T?) -> Unit, errorListener: BaseErrorListener
    ): WPComGsonRequest<JPTunnelWPComRestResponse<T>>? {
        val tunnelRequestUrl = getTunnelApiUrl(siteId)
        val wrappedType = TypeToken.getParameterized(JPTunnelWPComRestResponse::class.java, clazz.java).type
        val wrappedListener = Response.Listener<JPTunnelWPComRestResponse<T>> { listener(it.data) }

        val wrappedParams = createTunnelParams(params, wpApiEndpoint)

        return WPComGsonRequest.buildGetRequest(tunnelRequestUrl, wrappedParams, wrappedType,
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
