package org.wordpress.android.ui.reader.actions

import dagger.Reusable
import org.wordpress.android.models.ReaderPost
import javax.inject.Inject

@Reusable
class ReaderPostActionsWrapper @Inject constructor() {
    fun addToBookmarked(post: ReaderPost) = ReaderPostActions.addToBookmarked(post)
    fun removeFromBookmarked(post: ReaderPost) = ReaderPostActions.removeFromBookmarked(post)
    fun performLikeAction(
        post: ReaderPost?,
        isAskingToLike: Boolean,
        wpComUserId: Long
    ): Boolean = ReaderPostActions.performLikeAction(post, isAskingToLike, wpComUserId)
}
