package org.wordpress.android.datasets

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class to handle asynchronous I/O tasks using coroutines
 * @see <a href="https://github.com/wordpress-mobile/WordPress-Android/pull/20937">Introduction</a>
 */
object AsyncTaskExecutor {
    /**
     * Execute a data loading task in the IO thread and handle the result on the main thread
     */
    @JvmStatic
    fun <T> executeIo(scope: CoroutineScope, backgroundTask: () -> T, callback: AsyncTaskCallback<T>) {
        execute(scope, Dispatchers.IO, backgroundTask, callback)
    }

    /**
     * Execute a data loading task in the default thread and handle the result on the main thread
     */
    @JvmStatic
    fun <T> executeDefault(scope: CoroutineScope, backgroundTask: () -> T, callback: AsyncTaskCallback<T>) {
        execute(scope, Dispatchers.Default, backgroundTask, callback)
    }

    private fun <T> execute(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        backgroundTask: () -> T,
        callback: AsyncTaskCallback<T>
    ) {
        scope.launch(dispatcher) {
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

