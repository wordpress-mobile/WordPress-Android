package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobParameters
import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FORCE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER

/**
 * This class contains logic related to fetching data for the discover tab in the Reader.
 */
class ReaderDiscoverLogic constructor(private val completionListener: ServiceCompletionListener) {
    enum class DiscoverTasks {
        REQUEST, REQUEST_FORCE
    }

    private var listenerCompanion: JobParameters? = null

    fun performTasks(task: DiscoverTasks, companion: JobParameters?) {
        listenerCompanion = companion

        when (task) {
            REQUEST -> {
                requestDataForDiscover(false, UpdateResultListener {
                    // TODO malinjir emit REQUEST finish event
                    completionListener.onCompleted(listenerCompanion)
                })
            }
            REQUEST_FORCE -> {
                requestDataForDiscover(true, UpdateResultListener {
                    // TODO malinjir emit REQUEST_FORCE finish event
                    completionListener.onCompleted(listenerCompanion)
                })
            }
        }
    }

    private fun requestDataForDiscover(forceRefresh: Boolean, resultListener: ReaderActions.UpdateResultListener) {
        val path = "read/tags/cards"

        val sb = StringBuilder(path)

        if (!forceRefresh) {
            sb.append("?page_handle=")
            // TODO malinjir load page handle
        }
        val listener = Listener { jsonObject -> // remember when this tag was updated if newer posts were requested
            if (forceRefresh) {
                // TODO malinjir clear cache
            }
            handleRequestDiscoverDataResponse(jsonObject, resultListener)
        }
        val errorListener = ErrorListener { volleyError ->
            AppLog.e(READER, volleyError)
            resultListener.onUpdateResult(FAILED)
        }

        WordPress.getRestClientUtilsV2()[sb.toString(), null, null, listener, errorListener]
    }

    private fun handleRequestDiscoverDataResponse(json: JSONObject?, resultListener: UpdateResultListener) {
        if (json == null) {
            resultListener.onUpdateResult(FAILED)
            return
        }
        // TODO malinjir Parse data
        // TODO malinjir Save data into db
        // TODO malinjir Save next page handle
    }
}
