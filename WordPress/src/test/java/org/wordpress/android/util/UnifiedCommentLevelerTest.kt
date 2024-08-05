package org.wordpress.android.util

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.ui.comments.unified.UnifiedCommentLeveler

class UnifiedCommentLevelerTest {
    private val firstTopLevelComment = generateDummyComment(
        id = 1,
        remoteCommentId = 1,
        parentId = 0,
        hasParent = false
    )

    private val firstSecondLevelComment = generateDummyComment(
        id = 2,
        remoteCommentId = 2,
        parentId = 1,
        hasParent = true
    )

    private val secondSecondLevelComment = generateDummyComment(
        id = 3,
        remoteCommentId = 3,
        parentId = 1,
        hasParent = true
    )

    private val firstThirdLevelComment = generateDummyComment(
        id = 4,
        remoteCommentId = 4,
        parentId = 2,
        hasParent = true
    )

    private val firstFourthLevelComment = generateDummyComment(
        id = 5,
        remoteCommentId = 5,
        parentId = 4,
        hasParent = true
    )

    private val orphanedComment = generateDummyComment(id = 6, remoteCommentId = 6, parentId = 99, hasParent = true)

    private val listOfComments = listOf(
        firstTopLevelComment,
        firstSecondLevelComment,
        secondSecondLevelComment,
        firstThirdLevelComment,
        firstFourthLevelComment,
        orphanedComment
    )

    private lateinit var commentLeveler: UnifiedCommentLeveler

    @Before
    fun setup() {
        commentLeveler = UnifiedCommentLeveler(listOfComments)
    }

    @Test
    fun `createLevelList creates list of comments with correct level variable`() {
        val leveledCommentList = commentLeveler.createLevelList()

        val leveledFirstTopLevelComment = leveledCommentList.find { it == firstTopLevelComment }
        Assert.assertNotNull(leveledFirstTopLevelComment)
        Assert.assertEquals(0, leveledFirstTopLevelComment!!.level)

        val leveledFirstSecondLevelComment = leveledCommentList.find { it == firstSecondLevelComment }
        Assert.assertNotNull(leveledFirstSecondLevelComment)
        Assert.assertEquals(1, leveledFirstSecondLevelComment!!.level)

        val leveledSecondSecondLevelComment = leveledCommentList.find { it == secondSecondLevelComment }
        Assert.assertNotNull(leveledSecondSecondLevelComment)
        Assert.assertEquals(1, leveledSecondSecondLevelComment!!.level)

        val leveledFirstThirdLevelComment = leveledCommentList.find { it == firstThirdLevelComment }
        Assert.assertNotNull(leveledFirstThirdLevelComment)
        Assert.assertEquals(2, leveledFirstThirdLevelComment!!.level)

        val leveledFirstFourthLevelComment = leveledCommentList.find { it == firstFourthLevelComment }
        Assert.assertNotNull(leveledFirstFourthLevelComment)
        Assert.assertEquals(3, leveledFirstFourthLevelComment!!.level)

        val leveledOrphanedComment = leveledCommentList.find { it == orphanedComment }
        Assert.assertNotNull(leveledOrphanedComment)
        Assert.assertEquals(1, leveledOrphanedComment!!.level)
    }

    private fun generateDummyComment(id: Long, remoteCommentId: Long, parentId: Long, hasParent: Boolean) =
        CommentEntity(
            id = id,
            remoteCommentId = remoteCommentId,
            parentId = parentId,
            hasParent = hasParent,
            authorEmail = "",
            authorName = "",
            authorProfileImageUrl = "",
            authorUrl = "",
            content = "",
            datePublished = "",
            iLike = false,
            localSiteId = 0,
            postTitle = "",
            publishedTimestamp = 0L,
            authorId = 0,
            remotePostId = 0L,
            remoteSiteId = 0L,
            status = "",
            url = ""
        )
}
