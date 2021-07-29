package org.wordpress.android.ui.comments.unified

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class UnrepliedCommentsUtils @Inject constructor(
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    fun getUnrepliedComments(comments: CommentEntityList): CommentEntityList {
        val leveler = UnifiedCommentLeveler(comments)
        val leveledComments = leveler.createLevelList()
        val topLevelComments = java.util.ArrayList<CommentEntity>()
        for (comment in leveledComments) {
            // only check top level comments
            if (comment.level == 0) {
                val childrenComments: java.util.ArrayList<CommentEntity> = leveler.getChildren(comment.remoteCommentId)
                // comment is not mine and has no replies
                if (!isMyComment(comment) && childrenComments.isEmpty()) {
                    topLevelComments.add(comment)
                } else if (!isMyComment(comment)) { // comment is not mine and has replies
                    var hasMyReplies = false
                    for (childrenComment in childrenComments) { // check if any replies are mine
                        if (isMyComment(childrenComment)) {
                            hasMyReplies = true
                            break
                        }
                    }
                    if (!hasMyReplies) {
                        topLevelComments.add(comment)
                    }
                }
            }
        }
        return topLevelComments
    }

    private fun isMyComment(comment: CommentEntity): Boolean {
        val myEmail: String
        val selectedSite = selectedSiteRepository.getSelectedSite() ?: return false

        // if site is self hosted, we want to use email associate with it, even if we are logged into wpcom
        myEmail = if (!selectedSite.isUsingWpComRestApi) {
            selectedSite.email
        } else {
            val account: AccountModel = accountStore.account
            account.email
        }
        return comment.authorEmail == myEmail
    }
}
