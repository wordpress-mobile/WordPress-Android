package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import org.wordpress.android.ui.reader.ReaderConstants
import kotlin.coroutines.CoroutineContext

abstract class ReaderRepositoryDispatchingUseCase(
    private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + parentJob + coroutineExceptionHandler

    private val parentJob = Job()

    // todo: annmarie expose the exception up the chain perhaps by using a listener
    private val coroutineExceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
            }

    fun stop() {
        parentJob.cancelChildren()
    }

    companion object {
        const val EXCLUDE_TEXT_COLUMN = true
        const val MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
    }
}
