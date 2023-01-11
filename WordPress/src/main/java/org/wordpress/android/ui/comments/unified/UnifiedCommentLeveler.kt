package org.wordpress.android.ui.comments.unified

import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.COMMENTS

/**
 * Adaptation of ReaderCommentLeveler. We should combine them together as part of Comment Unification.
 */
class UnifiedCommentLeveler(private val mComments: List<CommentEntity>) {
    fun createLevelList(): ArrayList<CommentEntity> {
        val result = ArrayList<CommentEntity>()

        // reset all levels, and add root comments to result
        for (comment in mComments) {
            comment.level = 0
            if (comment.parentId == 0L) {
                result.add(comment)
            }
        }

        // add children at each level
        var level = 0
        while (walkCommentsAtLevel(result, level)) {
            level++
        }

        // check for orphans (child comments whose parents weren't found above) and give them
        // a non-zero level to distinguish them from top level comments
        val remainingComments = ArrayList(mComments)
        remainingComments.removeAll(result)

        for (comment in remainingComments) {
            if (!hasParent(comment)) {
                comment.level = 1
                result.add(comment)
                AppLog.d(COMMENTS, "Orphan comment encountered")
            }
        }
        return result
    }

    /*
     * walk comments in the passed list that have the passed level and add their children
     * beneath them
     */
    private fun walkCommentsAtLevel(comments: ArrayList<CommentEntity>, level: Int): Boolean {
        var hasChanges = false
        var index = 0
        while (index < comments.size) {
            val parent = comments[index]
            if (parent.level == level && hasChildren(parent.remoteCommentId)) {
                // get children for this comment, set their level, then add them below the parent
                val children = getChildren(parent.remoteCommentId)
                setLevel(children, level + 1)
                comments.addAll(index + 1, children)
                hasChanges = true
                // skip past the children we just added
                index += children.size
            }
            index++
        }
        return hasChanges
    }

    private fun hasChildren(commentId: Long): Boolean {
        for (comment in mComments) {
            if (comment.parentId == commentId) {
                return true
            }
        }
        return false
    }

    private fun hasParent(comment: CommentEntity): Boolean {
        for (parentComment in mComments) {
            if (parentComment.remoteCommentId == comment.parentId) {
                return true
            }
        }
        return false
    }

    fun getChildren(commentId: Long): ArrayList<CommentEntity> {
        val children = ArrayList<CommentEntity>()
        for (comment in mComments) {
            if (comment.parentId == commentId) {
                children.add(comment)
            }
        }
        return children
    }

    private fun setLevel(comments: ArrayList<CommentEntity>, level: Int) {
        for (comment in comments) {
            comment.level = level
        }
    }
}
