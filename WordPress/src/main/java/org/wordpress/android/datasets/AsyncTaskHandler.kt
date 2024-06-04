package org.wordpress.android.datasets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class to handle async tasks by using coroutines
 */
object AsyncTaskHandler {
    /**
     * Load data in the background and handle the result on the main thread
     */
    @JvmStatic
    fun <T> load(backgroundTask: () -> T, callback: AsyncTaskCallback<T>) {
        CoroutineScope(Dispatchers.IO).launch {
            // handle the background task
            val result = backgroundTask()

            withContext(Dispatchers.Main) {
                // handle the result on the main thread
                callback.onTaskFinished(result)
            }
        }
    }

    interface AsyncTaskCallback<T> {
        fun onTaskFinished(result: T)
    }
}

