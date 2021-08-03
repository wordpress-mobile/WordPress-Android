package org.wordpress.android.ui.comments

import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.models.CommentList
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import java.util.ArrayList

/**
 * Adaptation of ReaderCommentLeveler. We should combine them together as part of Comment Unification.
 */
@Deprecated("Comments are being refactored as part of Comments Unification project. If you are adding any" +
        " features or modifying this class, please ping develric or klymyam")
class CommentLeveler(private val mComments: List<CommentModel>) {
    fun createLevelList(): ArrayList<CommentModel> {
        val result = ArrayList<CommentModel>()

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
                AppLog.d(READER, "Orphan comment encountered")
            }
        }
        return result
    }

    /*
     * walk comments in the passed list that have the passed level and add their children
     * beneath them
     */
    private fun walkCommentsAtLevel(comments: ArrayList<CommentModel>, level: Int): Boolean {
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

    private fun hasParent(comment: CommentModel): Boolean {
        for (parentComment in mComments) {
            if (parentComment.remoteCommentId == comment.parentId) {
                return true
            }
        }
        return false
    }

    fun getChildren(commentId: Long): CommentList {
        val children = CommentList()
        for (comment in mComments) {
            if (comment.parentId == commentId) {
                children.add(comment)
            }
        }
        return children
    }

    private fun setLevel(comments: CommentList, level: Int) {
        for (comment in comments) {
            comment.level = level
        }
    }
}
