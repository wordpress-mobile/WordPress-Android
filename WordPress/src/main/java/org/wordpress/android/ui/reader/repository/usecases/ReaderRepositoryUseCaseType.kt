package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.wordpress.android.ui.reader.ReaderConstants
import kotlin.coroutines.CoroutineContext

abstract class ReaderRepositoryDispatchingUseCase(
    private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + parentJob + coroutineExceptionHandler

    val parentJob = SupervisorJob() // a job that can cancel all its children at once

    // todo: annmarie expose the exception up the chain perhaps by using a listener
    private val coroutineExceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
            }

    // todo: annmarie this probably shouldn't be in this class
    companion object {
        const val EXCLUDE_TEXT_COLUMN = true
        const val MAX_ROWS = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
    }

    fun stop() {
        parentJob.cancelChildren()
    }
}

enum class ReaderRepositoryUseCaseType {
    FETCH_POSTS_BY_TAG,
    FETCH_NUM_POSTS_BY_TAG,
    SHOULD_AUTO_UDPATE_TAG,
    FETCH_POSTS_BY_TAG_WITH_COUNT
}
