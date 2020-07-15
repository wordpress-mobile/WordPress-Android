package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import kotlin.coroutines.CoroutineContext

abstract class ReaderRepositoryDispatchingUseCase(
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = ioDispatcher + parentJob + coroutineExceptionHandler

    private val parentJob = Job()

    // todo: annmarie expose the exception up the chain perhaps by using a listener
    private val coroutineExceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
            }

    open fun stop() {
        parentJob.cancel()
    }

    companion object {
        const val EXCLUDE_TEXT_COLUMN = true
        const val MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
    }
}
// todo: annmarie - this should be removed - it's a helper log fun for coroutines
fun logCoroutine(methodName: String, coroutineContext: CoroutineContext) {
    AppLog.d(T.READER, "logCoroutine Thread for $methodName ${Thread.currentThread().name}" +
            " and the context is $coroutineContext")
}
