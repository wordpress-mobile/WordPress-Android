package org.wordpress.android.usecase.social

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetJetpackSocialShareMessageUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val postStore: PostStore,
) {
    suspend fun execute(localPostId: Int): String =
        withContext(ioDispatcher) {
            postStore.getPostByLocalPostId(localPostId)?.run {
                autoShareMessage.ifEmpty { title }
            } ?: ""
        }
}
