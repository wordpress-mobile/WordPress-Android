package org.wordpress.android.ui.reader.services.post

import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import javax.inject.Inject

class ReaderPostLogicFactory @Inject constructor(
    private val readerPostRepository: ReaderPostRepository,
) {
    fun create(listener: ServiceCompletionListener): ReaderPostLogic = ReaderPostLogic(
        listener,
        readerPostRepository,
    )
}
