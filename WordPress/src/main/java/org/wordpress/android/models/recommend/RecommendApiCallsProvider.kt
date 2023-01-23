package org.wordpress.android.models.recommend

import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Success
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.RestClientProvider
import org.wordpress.android.util.VolleyUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.RecommendAppSource
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RecommendApiCallsProvider @Inject constructor(
    private val contextProvider: ContextProvider,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val restClientProvider: RestClientProvider,
    private val localeManagerWrapper: LocaleManagerWrapper
) {
    suspend fun getRecommendTemplate(
        appName: String,
        source: RecommendAppSource
    ): RecommendCallResult = suspendCoroutine { cont ->
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            logErrorAndTrack(source, "getRecommendTemplate > No Network available")
            cont.resume(Failure(contextProvider.getContext().getString(R.string.no_network_message)))
        } else {
            val language = localeManagerWrapper.getLanguage()
            val endPointPath = "/mobile/share-app-link?app=$appName&locale=$language"

            val listener = Listener { jsonObject ->
                val result = getTemplateFromJson(jsonObject, appName, source)
                cont.resume(result)
            }
            val errorListener = ErrorListener { volleyError ->
                val (errorMessage, errorLog) = getNetErrorAndLogStrings("getRecommendTemplate", volleyError)
                logErrorAndTrack(source, errorLog)
                cont.resume(Failure(errorMessage))
            }

            restClientProvider.getRestClientUtilsV2().get(
                endPointPath,
                listener,
                errorListener
            )
        }
    }

    private fun logErrorAndTrack(source: RecommendAppSource, logMessage: String?) {
        val notNullMessage = logMessage ?: "logErrorAndTrack > logMessage was null"
        AppLog.d(T.API, notNullMessage)
        analyticsUtilsWrapper.trackRecommendAppFetchFailed(source, notNullMessage)
    }

    private fun getTemplateFromJson(
        json: JSONObject?,
        appName: String,
        source: RecommendAppSource
    ): RecommendCallResult {
        return if (json != null) {
            val name = json.optString("name")
            if (name == appName) {
                try {
                    val gson = Gson()
                    val mapType = object : TypeToken<RecommendTemplateData>() {}.type
                    val template = gson.fromJson<RecommendTemplateData>(json.toString(), mapType)
                    AppLog.d(
                        T.API,
                        "getTemplateFromJson > name[${template.name}], " +
                                "link[${template.link}], message[${template.message}]"
                    )
                    Success(template)
                } catch (jsonEx: JsonParseException) {
                    val logMessage = "getTemplateFromJson > Error parsing server API" +
                            " response: error[{${jsonEx.message}} json[$json]]"
                    logErrorAndTrack(source, logMessage)
                    Failure(
                        contextProvider.getContext()
                            .getString((R.string.recommend_app_bad_format_response))
                    )
                }
            } else {
                val logMessage = "getTemplateFromJson > wrong app name received: expected[$appName] got[$name]"
                logErrorAndTrack(source, logMessage)
                Failure(contextProvider.getContext().getString(R.string.recommend_app_bad_format_response))
            }
        } else {
            val logMessage = "getTemplateFromJson > null response received"
            logErrorAndTrack(source, logMessage)
            Failure(contextProvider.getContext().getString(R.string.recommend_app_null_response))
        }
    }

    private fun getNetErrorAndLogStrings(
        callingFunction: String,
        volleyError: VolleyError?
    ): Pair<String, String> {
        val error = VolleyUtils.errStringFromVolleyError(volleyError)
        return if (error.isNullOrEmpty()) {
            Pair(
                contextProvider.getContext().getString(R.string.recommend_app_generic_get_template_error),
                "$callingFunction > Failed with empty string [volleyError = $volleyError]"
            )
        } else {
            Pair(
                error,
                "$callingFunction > Failed [error = $error]"
            )
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
