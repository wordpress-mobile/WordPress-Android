package org.wordpress.android.ui.reader.services.discover

import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import java.util.EnumSet

/**
 * This class contains logic related to fetching data for the discover tab in the Reader.
 */
class ReaderDiscoverLogic constructor(private val completionListener: ServiceCompletionListener) {
    enum class DiscoverTasks {
        REQUEST_REFRESH, REQUEST_OLDER
    }

    private var currentTasks: EnumSet<DiscoverTasks>? = null
    private var mListenerCompanion: Any? = null

    fun performTasks(tasks: EnumSet<DiscoverTasks>, companion: Any?) {
        currentTasks = EnumSet.copyOf(tasks)
        mListenerCompanion = companion
    }

    private fun taskCompleted(task: DiscoverTasks) {
        currentTasks!!.remove(task)
        if (currentTasks!!.isEmpty()) {
            allTasksCompleted()
        }
    }
    private fun allTasksCompleted() {
        AppLog.i(READER, "reader service > all tasks completed")
        completionListener.onCompleted(mListenerCompanion)
    }
}
