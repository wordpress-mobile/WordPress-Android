package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostConflictDetector
import javax.inject.Inject

@Suppress("LongParameterList")
class PageConflictDetector @Inject constructor(
   private val postConflictDetector: PostConflictDetector
) {
    fun hasUnhandledConflict(post: PostModel) = postConflictDetector.hasUnhandledConflict(post)

    fun hasUnhandledAutoSave(post: PostModel) = postConflictDetector.hasUnhandledAutoSave(post)
}
