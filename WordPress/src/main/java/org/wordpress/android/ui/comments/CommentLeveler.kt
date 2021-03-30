package org.wordpress.android.ui.comments

import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.models.CommentList
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER

/**
 * Adaptation of ReaderCommentLeveler. We should combine them together as part of Comment Unification.
 */
class CommentLeveler(private val mComments: List<CommentModel>) {
    fun createLevelList(): CommentList {
        val result = CommentList()

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
        for (comment in result) {
            if (comment.level == 0 && comment.parentId != 0L) {
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
    private fun walkCommentsAtLevel(comments: CommentList, level: Int): Boolean {
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
