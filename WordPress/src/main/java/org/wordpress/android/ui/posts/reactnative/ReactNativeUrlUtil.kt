package org.wordpress.android.ui.posts.reactnative

import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.EDITOR
import java.lang.IndexOutOfBoundsException
import javax.inject.Inject

private const val WPCOM_ENDPOINT = "https://public-api.wordpress.com"

class ReactNativeUrlUtil @Inject constructor() {
    internal fun parseUrlAndParamsForWPCom(
        pathWithParams: String,
        wpComSiteId: Long
    ): Pair<String, Map<String, String>>? =
            parsePathAndParams(pathWithParams)?.let { (path, params) ->
                val url = WPCOM_ENDPOINT + path.replace("wp/v2/", "wp/v2/sites/$wpComSiteId/")
                Pair(url, params)
            }

    internal fun parseUrlAndParamsForWPOrg(
        pathWithParams: String,
        siteUrl: String
    ): Pair<String, Map<String, String>>? =
            parsePathAndParams(pathWithParams)?.let { (path, params) ->
                val url = "$siteUrl/wp-json$path"
                val updatedParams = updateUrlForWPOrgRequest(params)
                Pair(url, updatedParams)
            }

    /*
     * We cannot make authorized requests to self-hosted sites. A "context" of
     * "edit" requires authorization, so changing the context to "view" since that does not need
     * authorizations and still has the data we need.
     */
    private fun updateUrlForWPOrgRequest(params: Map<String, String>): Map<String, String> =
            params.mapValues { (key, value) ->
                val hasEditContext = key == "context" && value == "edit"
                if (hasEditContext) {
                    "view"
                } else {
                    value
                }
            }

    private fun parsePathAndParams(pathWithParams: String): Pair<String, Map<String, String>>? =
            try {
                if (pathWithParams.contains("?")) {
                    val (path, params) = pathWithParams.split("?")
                    val paramsMap = params.split("&").map {
                        val (key, value) = it.split("=")
                        key to value
                    }.toMap()
                    Pair(path, paramsMap)
                } else {
                    Pair(pathWithParams, emptyMap())
                }
            } catch (e: IndexOutOfBoundsException) {
                AppLog.e(EDITOR, e)
                null
            }
}
