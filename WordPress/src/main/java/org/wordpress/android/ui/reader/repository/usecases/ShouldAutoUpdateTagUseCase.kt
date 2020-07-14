package org.wordpress.android.ui.reader.repository.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class ShouldAutoUpdateTagUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    suspend fun get(readerTag: ReaderTag): Boolean =
            withContext(coroutineContext) {
                ReaderTagTable.shouldAutoUpdateTag(readerTag)
            }
}
