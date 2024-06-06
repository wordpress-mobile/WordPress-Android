package org.wordpress.android.datasets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class to handle asynchronous I/O tasks using coroutines
 * @see <a href="https://github.com/wordpress-mobile/WordPress-Android/pull/20937">Introduction</a>
 */
object AsyncIoTaskExecutor {
    /**
     * Execute a data loading task in the IO thread and handle the result on the main thread
     */
    @JvmStatic
    fun <T> execute(scope: CoroutineScope, backgroundTask: () -> T, callback: IoTaskResultCallback<T>) {
        scope.launch {
            // handle the background task
            val result = withContext(Dispatchers.IO) {
                backgroundTask()
            }

            withContext(Dispatchers.Main) {
                // handle the result on the main thread
                callback.onTaskFinished(result)
            }
        }
    }

    interface IoTaskResultCallback<T> {
        fun onTaskFinished(result: T)
    }
}

