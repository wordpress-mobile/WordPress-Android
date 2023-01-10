package org.wordpress.android.ui.reader.utils

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase
import javax.inject.Inject
import javax.inject.Named

/**
 * Injectable wrapper around ReaderSeenStatusToggleUseCase.
 *
 * ReaderSeenStatusToggleUseCase uses suspended functions and flows, which makes calling if from Java clunky.
 *
 */
@Reusable
class ReaderPostSeenStatusWrapper @Inject constructor(
    private val seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext = bgDispatcher + Job()

    fun markPostAsSeenSilently(post: ReaderPost) {
        launch(bgDispatcher) {
            seenStatusToggleUseCase.markPostAsSeenIfNecessary(post)
        }
    }
}
