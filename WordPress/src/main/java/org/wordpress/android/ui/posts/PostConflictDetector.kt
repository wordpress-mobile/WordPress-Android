package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.UploadStore
import javax.inject.Inject

@Suppress("LongParameterList")
class PostConflictDetector @Inject constructor (
    private val uploadStore: UploadStore
) {
    fun hasUnhandledConflict(post: PostModel): Boolean =
        uploadStore.getUploadErrorForPost(post)?.postError?.type == PostErrorType.OLD_REVISION ||
                PostUtils.isPostInConflictWithRemote(post)

    fun hasUnhandledAutoSave(post: PostModel) = PostUtils.hasAutoSave(post)
}
