package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostUtils
import javax.inject.Inject

class AutoSaveConflictResolver @Inject constructor() {
    fun hasUnhandledAutoSave(post: PostModel): Boolean {
        return PostUtils.hasAutoSave(post)
    }
}
