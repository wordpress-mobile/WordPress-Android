package org.wordpress.android.models.recommend

import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Success
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.VolleyUtils
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RecommendApiCallsProvider @Inject constructor(
    private val contextProvider: ContextProvider
) {
    suspend fun getRecommendTemplate(appName: String): RecommendCallResult = suspendCoroutine { cont ->
        val language = LocaleManager.getLanguage(contextProvider.getContext())
        val endPointPath = "/mobile/share-app-link?app=$appName&locale=$language"

        val listener = Listener { jsonObject ->
            val result = getTemplateFromJson(jsonObject, appName)
            cont.resume(result)
        }
        val errorListener = ErrorListener { volleyError ->
            val error = getErrorStringAndLog("getRecommendTemplate", volleyError)
            cont.resume(Failure(error))
        }

        WordPress.getRestClientUtilsV2().get(
                endPointPath,
                listener,
                errorListener
        )
    }

    private fun getTemplateFromJson(json: JSONObject?, appName: String): RecommendCallResult {
        return json?.let {
            val name = it.optString("name")
            if (name == appName) {
                try {
                    val gson = Gson()
                    val mapType = object : TypeToken<RecommendTemplateData>() {}.type
                    val template = gson.fromJson<RecommendTemplateData>(it.toString(), mapType)
                    AppLog.d(
                            T.API,
                            "getTemplateFromJson > name[${template.name}], " +
                                    "link[${template.link}], message[${template.message}]"
                    )
                    Success(template)
                } catch (jsonEx: JsonParseException) {
                    AppLog.d(
                            T.API,
                            "getTemplateFromJson > Error parsing server API response: error[{${jsonEx.message}}]"
                    )
                    Failure(
                            contextProvider.getContext()
                                    .getString((R.string.recommend_app_bad_format_response))
                    )
                }
            } else {
                AppLog.d(T.API, "getTemplateFromJson > wrong app name received: expected[$appName] got[$name]")
                Failure(contextProvider.getContext().getString(R.string.recommend_app_bad_format_response))
            }
        } ?: Failure(contextProvider.getContext().getString(R.string.recommend_app_null_response))
    }

    private fun getErrorStringAndLog(
        functionName: String,
        volleyError: VolleyError?
    ): String {
        val error = VolleyUtils.errStringFromVolleyError(volleyError)
        return if (error.isNullOrEmpty()) {
            AppLog.d(
                    T.API,
                    "$functionName > Failed with empty string [volleyError = $volleyError]"
            )
            contextProvider.getContext().getString(R.string.recommend_app_generic_get_template_error)
        } else {
            AppLog.d(
                    T.API,
                    "$functionName > Failed [error = $error]"
            )
            error
        }
    }

    sealed class RecommendCallResult {
        data class Success(val templateData: RecommendTemplateData) : RecommendCallResult()
        data class Failure(val error: String) : RecommendCallResult()
    }

    data class RecommendTemplateData(val name: String, val message: String, val link: String)

    enum class RecommendAppName(val appName: String) {
        WordPress("wordpress"),
        Jetpack("jetpack")
    }
}
