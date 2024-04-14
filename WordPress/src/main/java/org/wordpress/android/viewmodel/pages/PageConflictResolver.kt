package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.posts.PostUtils
import javax.inject.Inject

class PageConflictResolver @Inject constructor(
    private val uploadStore: UploadStore
) {
    fun hasUnhandledAutoSave(post: PostModel) = PostUtils.hasAutoSave(post)

    fun doesPageHaveUnhandledConflict(post: PostModel) =
        uploadStore.getUploadErrorForPost(post)?.postError?.type == PostStore.PostErrorType.OLD_REVISION ||
                PostUtils.isPostInConflictWithRemote(post)
}
