package org.wordpress.android.ui.reader.utils

import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderCommentList
import java.util.LinkedList

/*
 * utility class which accepts a list of comments and then creates a "level list" from it
 * which places child comments below their parents with indentation levels applied
 */
class ReaderCommentLeveler(private val mComments: ReaderCommentList) {
    fun createLevelList(): ReaderCommentList {
        val result = ReaderCommentList()


        mComments.distinctBy { it.commentId }.forEachIndexed { index, comment ->
            if (comment.parentId == 0L || isParrentMissing(comment)) {
                val depth = getLevel(comment)
                comment.level = depth
                result.add(comment)
                val commentsToSearch: LinkedList<Long> = LinkedList<Long>()
                commentsToSearch.addFirst(comment.commentId)
                while (!commentsToSearch.isEmpty()) {
                    for (j in index + 1 until mComments.size) {
                        val potentialNestedComment: ReaderComment = mComments[j]
                        if (potentialNestedComment.parentId == commentsToSearch.first) {
                            val nestedDepth = getLevel(potentialNestedComment)
                            potentialNestedComment.level = nestedDepth
                            result.add(potentialNestedComment)
                            commentsToSearch.addLast(potentialNestedComment.commentId)
                        }
                    }
                    commentsToSearch.removeFirst()
                }
            }
        }

        result.forEachIndexed { index, readerComment ->
            if (readerComment.parentId > 0L && isParrentMissing(readerComment)) {
                readerComment.isOrphan = true

                if (index > 0) {
                    val previousComment = result[index - 1]
                    readerComment.isNestedOrphan = previousComment.parentId == readerComment.parentId
                }
            }
        }

        return result
    }

    private fun getComment(id: Long):ReaderComment? {
       return mComments.firstOrNull { it.commentId == id }
    }

    private fun getLevel(comment: ReaderComment?): Int{
        if(comment == null) return 0

        if(comment.parentId == 0L) return 0

        return getLevel(getComment(comment.parentId)) + 1
    }

    /*
     * walk comments in the passed list that have the passed level and add their children
     * beneath them
     */
    private fun walkCommentsAtLevel(comments: ReaderCommentList, level: Int): Boolean {
        var hasChanges = false
        var index = 0
        while (index < comments.size) {
            val parent = comments[index]
            if (parent.level == level && hasChildren(parent.commentId)) {
                // get children for this comment, set their level, then add them below the parent
                val children = getChildren(parent.commentId)
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

    private fun getChildren(commentId: Long): ReaderCommentList {
        val children = ReaderCommentList()
        for (comment in mComments) {
            if (comment.parentId == commentId) {
                children.add(comment)
            }
        }
        return children
    }

    private fun setLevel(comments: ReaderCommentList, level: Int) {
        for (comment in comments) {
            comment.level = level
        }
    }

    private fun isParrentMissing(comment: ReaderComment): Boolean {
        for (parentComment in mComments) {
            if (parentComment.commentId == comment.parentId) {
                return false
            }
        }
        return true
    }

    private fun getParent(comment: ReaderComment): ReaderComment? {
        for (parentComment in mComments) {
            if (parentComment.commentId == comment.parentId) {
                return parentComment
            }
        }
        return null
    }
}
