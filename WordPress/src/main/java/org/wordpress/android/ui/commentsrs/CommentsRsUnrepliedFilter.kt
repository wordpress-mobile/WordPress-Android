package org.wordpress.android.ui.commentsrs

import org.wordpress.android.ui.comments.unified.CommentsRsDataSource.RsComment

/**
 * Client-side "Unreplied" filter for the rs comments list, mirroring the legacy
 * `UnrepliedCommentsUtils`. A comment is unreplied when it is a top-level comment
 * ([RsComment.parentId] == 0) that the current user did not write and that has no direct reply
 * written by the current user.
 *
 * Threading is computed over whatever comments have been loaded so far, so — exactly like the
 * legacy list — a reply that lives on a not-yet-loaded page can briefly leave its parent showing
 * as unreplied until that page arrives.
 *
 * "Mine" is a user-id comparison ([RsComment.authorId] == [myUserId]) because the view context
 * carries no author email. When [myUserId] is null (the "me" fetch failed), nothing counts as
 * mine and the filter over-reports rather than hiding comments — the same fallback the legacy
 * util has when it can't resolve the account email.
 */
internal fun filterUnreplied(comments: List<RsComment>, myUserId: Long?): List<RsComment> {
    val myReplyParentIds = comments
        .filter { it.parentId != 0L && isMine(it, myUserId) }
        .map { it.parentId }
        .toSet()
    return comments.filter { comment ->
        comment.parentId == 0L &&
                !isMine(comment, myUserId) &&
                comment.remoteCommentId !in myReplyParentIds
    }
}

private fun isMine(comment: RsComment, myUserId: Long?): Boolean =
    myUserId != null && comment.authorId == myUserId
