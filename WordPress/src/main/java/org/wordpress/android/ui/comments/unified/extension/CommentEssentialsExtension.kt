package org.wordpress.android.ui.comments.unified.extension

import org.wordpress.android.ui.comments.unified.CommentEssentials

fun CommentEssentials.isNotEqualTo(other: CommentEssentials): Boolean {
    return !(this.commentText == other.commentText &&
            this.userEmail == other.userEmail &&
            this.userName == other.userName &&
            this.userUrl == other.userUrl)
}
