package org.wordpress.android.util

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.ui.comments.CommentLeveler

class CommentLevelerTest {
    private val firstTopLevelComment = CommentModel().apply {
        remoteCommentId = 1
        id = 1
        parentId = 0
        hasParent = false
    }

    private val firstSecondLevelComment = CommentModel().apply {
        remoteCommentId = 2
        id = 2
        parentId = 1
        hasParent = true
    }

    private val secondSecondLevelComment = CommentModel().apply {
        remoteCommentId = 3
        id = 3
        parentId = 1
        hasParent = true
    }

    private val firstThirdLevelComment = CommentModel().apply {
        remoteCommentId = 4
        id = 4
        parentId = 2
        hasParent = true
    }

    private val firstFourthLevelComment = CommentModel().apply {
        remoteCommentId = 5
        id = 5
        parentId = 4
        hasParent = true
    }

    private val orphanedComment = CommentModel().apply {
        remoteCommentId = 6
        id = 6
        parentId = 99 // parent does not exist
        hasParent = true
    }

    private val listOfComments = arrayListOf(
            firstTopLevelComment,
            firstSecondLevelComment,
            secondSecondLevelComment,
            firstThirdLevelComment,
            firstFourthLevelComment,
            orphanedComment
    )

    private lateinit var commentLeveler: CommentLeveler

    @Before
    fun setup() {
        commentLeveler = CommentLeveler(listOfComments)
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
}
