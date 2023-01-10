package org.wordpress.android.ui.comments.utils

import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData

val testComments = generateMockComments(100)

val testCommentsPayload30 = CommentsActionPayload(PagingData(comments = testComments.take(30), hasMore = true))
val testCommentsPayload60 = CommentsActionPayload(PagingData(comments = testComments.take(60), hasMore = true))
val testCommentsPayloadLastPage = CommentsActionPayload(PagingData(comments = testComments.take(90), hasMore = false))

val approvedComment = testComments[1].copy(status = APPROVED.toString())
val pendingComment = testComments[2].copy(status = UNAPPROVED.toString())
val trashedComment = testComments[3].copy(status = TRASH.toString())

fun generateMockComments(numComments: Int): List<CommentEntity> {
    val comments = ArrayList<CommentEntity>()
    for (i in 0..numComments) {
        comments.add(
            CommentEntity(
                id = i.toLong(),
                remoteCommentId = i.toLong(),
                remotePostId = i.toLong() * 2,
                authorId = i.toLong() * 3,
                localSiteId = 5,
                remoteSiteId = 95,
                authorUrl = "http://author$i.org",
                authorName = "Author $i",
                authorEmail = "author$i@wordpress.org",
                authorProfileImageUrl = "http://author$i.org/profile.jpg",
                postTitle = "POst $i",
                status = "approved",
                datePublished = "some date",
                publishedTimestamp = 0L,
                content = "Content $i",
                url = "",
                hasParent = false,
                parentId = 0L,
                iLike = false
            )
        )
    }
    return comments
}
