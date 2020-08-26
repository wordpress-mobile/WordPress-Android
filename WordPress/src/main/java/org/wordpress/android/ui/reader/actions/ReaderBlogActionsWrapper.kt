package org.wordpress.android.ui.reader.actions

import dagger.Reusable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import javax.inject.Inject

@Reusable
class ReaderBlogActionsWrapper @Inject constructor() {
    fun followBlogForPost(post: ReaderPost, isAskingToFollow: Boolean, actionListener: ActionListener) =
            ReaderBlogActions.followBlogForPost(post, isAskingToFollow, actionListener)
}
