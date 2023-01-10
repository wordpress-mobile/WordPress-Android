package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.fluxc.store.UploadStore.ClearMediaPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.posts.editor.AztecEditorFragmentStaticWrapper
import javax.inject.Inject
import javax.inject.Named

@Reusable
class CleanUpMediaToPostAssociationUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    private val uploadStore: UploadStore,
    private val aztecEditorWrapper: AztecEditorFragmentStaticWrapper,
    private val postUtilsWrapper: PostUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun purgeMediaToPostAssociationsIfNotInPostAnymore(post: PostImmutableModel) {
        withContext(bgDispatcher) {
            val mediaAssociatedWithPost = uploadStore.getFailedMediaForPost(post) +
                    uploadStore.getCompletedMediaForPost(post) +
                    uploadStore.getUploadingMediaForPost(post)

            mediaAssociatedWithPost
                .filter { media ->
                    // Find media which is not in the post anymore
                    val containsGutenbergBlocks = postUtilsWrapper.contentContainsGutenbergBlocks(post.content)
                    if (containsGutenbergBlocks) {
                        !postUtilsWrapper.isMediaInGutenbergPostBody(post.content, media.id.toString())
                    } else {
                        !aztecEditorWrapper.isMediaInPostBody(post.content, media.id.toString())
                    }
                }
                .filter { media ->
                    // Featured images are not in post content, don't delete them
                    !media.markedLocallyAsFeatured
                }
                .toSet()
                .let { mediaToDeleteAssociationFor ->
                    if (mediaToDeleteAssociationFor.isNotEmpty()) {
                        val clearMediaPayload = ClearMediaPayload(
                            post,
                            mediaToDeleteAssociationFor
                        )
                        dispatcher.dispatch(
                            UploadActionBuilder.newClearMediaForPostAction(
                                clearMediaPayload
                            )
                        )
                    }
                }
        }
    }
}
