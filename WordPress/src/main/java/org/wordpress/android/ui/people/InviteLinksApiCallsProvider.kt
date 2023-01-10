package org.wordpress.android.ui.people

import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult.Failure
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult.Success
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.VolleyUtils
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InviteLinksApiCallsProvider @Inject constructor(
    private val contextProvider: ContextProvider
) {
    suspend fun getInviteLinksStatus(blogId: Long): InviteLinksCallResult = suspendCoroutine { cont ->
        val endPointPath = "/sites/$blogId/invites"

        val listener = Listener { jsonObject ->
            val result = getLinks(jsonObject)
            AppLog.d(
                T.PEOPLE,
                "getInviteLinksStatus > Succeeded [blogId=$blogId - result = " +
                        "${(result as Success).links.map { "Name: ${it.role} Expiry: ${it.expiry}" }}]"
            )
            cont.resume(result)
        }
        val errorListener = ErrorListener { volleyError ->
            val error = getErrorStringAndLog("getInviteLinksStatus", blogId, volleyError)
            cont.resume(Failure(error))
        }

        WordPress.getRestClientUtilsV1_1().get(
            endPointPath,
            listener,
            errorListener
        )
    }

    suspend fun generateLinks(blogId: Long): InviteLinksCallResult = suspendCoroutine { cont ->
        val endPointPath = "/sites/$blogId/invites/links/generate"

        val listener = Listener {
            AppLog.d(
                T.PEOPLE,
                "generateLinks > Succeeded [blogId=$blogId]"
            )
            cont.resume(Success(listOf()))
        }
        val errorListener = ErrorListener { volleyError ->
            val error = getErrorStringAndLog("generateLinks", blogId, volleyError)
            cont.resume(Failure(error))
        }

        WordPress.getRestClientUtilsV2().post(
            endPointPath,
            listener,
            errorListener
        )
    }

    suspend fun disableLinks(blogId: Long): InviteLinksCallResult = suspendCoroutine { cont ->
        val endPointPath = "/sites/$blogId/invites/links/disable"

        val listener = Listener {
            AppLog.d(
                T.PEOPLE,
                "deleteLinks > Succeeded [blogId=$blogId]"
            )
            cont.resume(Success(listOf()))
        }
        val errorListener = ErrorListener { volleyError ->
            val error = getErrorStringAndLog("deleteLinks", blogId, volleyError)
            cont.resume(Failure(error))
        }

        WordPress.getRestClientUtilsV2().post(
            endPointPath,
            listener,
            errorListener
        )
    }

    private fun getLinks(json: JSONObject?): InviteLinksCallResult {
        return json?.let {
            if (it.has("links")) {
                try {
                    val linksSection = it.getJSONArray("links").toString()
                    val gson = Gson()
                    val mapType = object : TypeToken<Array<InviteLinksItem>>() {}.type
                    val linksData = gson.fromJson<Array<InviteLinksItem>>(linksSection, mapType).toList()

                    Success(linksData)
                } catch (jsonEx: JsonParseException) {
                    AppLog.d(T.PEOPLE, "getLinks > Error parsing server API response: error[{${jsonEx.message}}]")
                    Failure(contextProvider.getContext().getString((string.invite_links_bad_format_response)))
                }
            } else {
                Success(listOf())
            }
        } ?: Failure(contextProvider.getContext().getString(string.invite_links_null_response))
    }

    private fun getErrorStringAndLog(
        functionName: String,
        blogId: Long,
        volleyError: VolleyError?
    ): String {
        val error = VolleyUtils.errStringFromVolleyError(volleyError)
        return if (error.isNullOrEmpty()) {
            AppLog.d(
                T.PEOPLE,
                "$functionName > Failed with empty string " +
                        "[blogId=$blogId - volleyError = $volleyError]"
            )
            contextProvider.getContext().getString(R.string.invite_links_generic_get_data_error)
        } else {
            AppLog.d(
                T.PEOPLE,
                "$functionName > Failed [blogId=$blogId - error = $error]"
            )
            error
        }
    }

    data class InviteLinksItem(val role: String, val expiry: Long, val link: String /*UriWrapper*/)

    sealed class InviteLinksCallResult {
        data class Success(val links: List<InviteLinksItem>) : InviteLinksCallResult()
        data class Failure(val error: String) : InviteLinksCallResult()
    }
}
