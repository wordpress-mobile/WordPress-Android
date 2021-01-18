package org.wordpress.android.ui.reader.utils

import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import org.json.JSONArray
import org.json.JSONObject
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult.Failure
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult.Success
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val ANDROID_READER_SOURCE_PARAMETER_VALUE = "reader-android"

class PostSeenStatusApiCallsProvider @Inject constructor(
    private val contextProvider: ContextProvider
) {
    suspend fun markPostAsSeen(post: ReaderPost): SeenStatusToggleCallResult = suspendCoroutine { cont ->
        val requestJson = JSONObject()
        requestJson.put("feed_id", post.feedId.toString())
        requestJson.put("feed_item_ids", JSONArray(arrayListOf(post.feedItemId)))
        requestJson.put("source", ANDROID_READER_SOURCE_PARAMETER_VALUE)

        val listener = Listener { responseJson ->
            val result = seenStatusToggleSuccessful(responseJson, true)
            cont.resume(result)
        }
        val errorListener = ErrorListener { volleyError ->
            cont.resume(Failure("error"))
        }
        WordPress.getRestClientUtilsV2().post("/seen-posts/seen/new", requestJson, null, listener, errorListener)
    }

    suspend fun markPostAsUnseen(post: ReaderPost): SeenStatusToggleCallResult = suspendCoroutine { cont ->
        val requestJson = JSONObject()
        requestJson.put("feed_id", post.feedId.toString())
        requestJson.put("feed_item_ids", JSONArray(arrayListOf(post.feedItemId)))
        requestJson.put("source", ANDROID_READER_SOURCE_PARAMETER_VALUE)
        val listener = Listener { responseJson ->
            val result = seenStatusToggleSuccessful(responseJson, false)
            cont.resume(result)
        }
        val errorListener = ErrorListener { volleyError ->
            cont.resume(Failure("error"))
        }
        WordPress.getRestClientUtilsV2().post("/seen-posts/seen/delete", requestJson, null, listener, errorListener)
    }

    private fun seenStatusToggleSuccessful(json: JSONObject?, askedToMarkAsSeen: Boolean): SeenStatusToggleCallResult {
        return json?.let {
            val success = it.optBoolean("status", false)
            if (success) {
                Success(askedToMarkAsSeen)
            } else {
                Failure(contextProvider.getContext().getString(R.string.reader_follow_comments_bad_format_response))
            }
        } ?: Failure(contextProvider.getContext().getString(R.string.reader_follow_comments_null_response))
    }

    sealed class SeenStatusToggleCallResult {
        data class Success(val isSeen: Boolean) : SeenStatusToggleCallResult()
        data class Failure(val error: String) : SeenStatusToggleCallResult()
    }
}
