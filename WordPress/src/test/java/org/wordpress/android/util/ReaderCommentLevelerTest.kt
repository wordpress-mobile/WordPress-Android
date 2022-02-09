package org.wordpress.android.util

import org.junit.Assert
import org.junit.Test
import org.wordpress.android.models.ReaderComment
import org.wordpress.android.models.ReaderCommentList
import org.wordpress.android.ui.reader.utils.ReaderCommentLeveler

class ReaderCommentLevelerTest {
    private val firstIndependentComment = generateDummyComment(
            remoteCommentId = 1,
            parentId = 0
    )

    private val parentComment = generateDummyComment(
            remoteCommentId = 2,
            parentId = 0
    )

    private val replyToParentComment = generateDummyComment(
            remoteCommentId = 4,
            parentId = 2
    )

    private val secondReplyToParentComment = generateDummyComment(
            remoteCommentId = 5,
            parentId = 2
    )

    private val replyToSecondReplyToParentComment = generateDummyComment(
            remoteCommentId = 7,
            parentId = 5
    )

    private val replyToReplyToSecondReplyToParentComment = generateDummyComment(
            remoteCommentId = 8,
            parentId = 7
    )

    private val thirdReplyToParentComment = generateDummyComment(
            remoteCommentId = 6,
            parentId = 2
    )

    private val secondIndependentComment = generateDummyComment(
            remoteCommentId = 3,
            parentId = 0
    )

//    private val orphanedComment = generateDummyComment(remoteCommentId = 6, parentId = 99)

    private val listOfComments = ReaderCommentList().apply {
        addAll(
                listOf(
                        firstIndependentComment,
                        parentComment,
                        replyToParentComment,
                        secondReplyToParentComment,
                        replyToSecondReplyToParentComment,
                        replyToReplyToSecondReplyToParentComment,
                        thirdReplyToParentComment,
                        secondIndependentComment
                )
        )
    }

    @Test
    fun `given comment list without orphans createLevelList creates list of comments with correct level variable`() {
        val commentLeveler = ReaderCommentLeveler(listOfComments)

        val leveledCommentList = commentLeveler.createLevelList()

        val leveledFirstIndependentComment = leveledCommentList.find { it == firstIndependentComment }
        Assert.assertNotNull(leveledFirstIndependentComment)
        Assert.assertEquals(0, leveledFirstIndependentComment!!.level)

        val leveledParentComment = leveledCommentList.find { it == parentComment }
        Assert.assertNotNull(leveledParentComment)
        Assert.assertEquals(0, leveledParentComment!!.level)

        val leveledReplyToParentCommentComment = leveledCommentList.find { it == replyToParentComment }
        Assert.assertNotNull(leveledReplyToParentCommentComment)
        Assert.assertEquals(1, leveledReplyToParentCommentComment!!.level)

        val leveledSecondReplyToParentCommentComment = leveledCommentList.find { it == secondReplyToParentComment }
        Assert.assertNotNull(leveledSecondReplyToParentCommentComment)
        Assert.assertEquals(1, leveledSecondReplyToParentCommentComment!!.level)

        val leveledReplyToSecondReplyToParentComment = leveledCommentList.find { it == replyToSecondReplyToParentComment }
        Assert.assertNotNull(leveledReplyToSecondReplyToParentComment)
        Assert.assertEquals(2, leveledReplyToSecondReplyToParentComment!!.level)

        val leveledReplyToReplyToSecondReplyToParentComment = leveledCommentList.find { it == replyToReplyToSecondReplyToParentComment }
        Assert.assertNotNull(leveledReplyToReplyToSecondReplyToParentComment)
        Assert.assertEquals(3, leveledReplyToReplyToSecondReplyToParentComment!!.level)

        val leveledThirdReplyToParentComment = leveledCommentList.find { it == thirdReplyToParentComment }
        Assert.assertNotNull(leveledThirdReplyToParentComment)
        Assert.assertEquals(1, leveledThirdReplyToParentComment!!.level)

        val leveledSecondIndependentComment = leveledCommentList.find { it == secondIndependentComment }
        Assert.assertNotNull(leveledSecondIndependentComment)
        Assert.assertEquals(0, leveledSecondIndependentComment!!.level)
    }

    @Test
    fun `given comment list with single orphan createLevelList creates list of comments with correct level variable`() {
        var commentLeveler = ReaderCommentLeveler(listOfComments)
        val leveledCommentListWithoutOrphans = commentLeveler.createLevelList()

        leveledCommentListWithoutOrphans.remove(parentComment)

        val leveledParentComment = leveledCommentListWithoutOrphans.find { it == parentComment }
        Assert.assertNull(leveledParentComment)

        commentLeveler = ReaderCommentLeveler(leveledCommentListWithoutOrphans)
        val leveledCommentListWithSingleOrphan = commentLeveler.createLevelList()

        val leveledFirstIndependentComment = leveledCommentListWithSingleOrphan.find { it == firstIndependentComment }
        Assert.assertNotNull(leveledFirstIndependentComment)
        Assert.assertEquals(0, leveledFirstIndependentComment!!.level)
        Assert.assertFalse(leveledFirstIndependentComment.isOrphan)
        Assert.assertFalse(leveledFirstIndependentComment.isNestedOrphan)

        val leveledReplyToParentCommentComment = leveledCommentListWithSingleOrphan.find { it == replyToParentComment }
        Assert.assertNotNull(leveledReplyToParentCommentComment)
        Assert.assertEquals(1, leveledReplyToParentCommentComment!!.level)
        Assert.assertTrue(leveledReplyToParentCommentComment.isOrphan)
        Assert.assertFalse(leveledReplyToParentCommentComment.isNestedOrphan)

        val leveledSecondReplyToParentCommentComment = leveledCommentListWithSingleOrphan.find { it == secondReplyToParentComment }
        Assert.assertNotNull(leveledSecondReplyToParentCommentComment)
        Assert.assertEquals(1, leveledSecondReplyToParentCommentComment!!.level)
        Assert.assertTrue(leveledSecondReplyToParentCommentComment.isOrphan)
        Assert.assertTrue(leveledSecondReplyToParentCommentComment.isNestedOrphan)

        val leveledReplyToSecondReplyToParentComment = leveledCommentListWithSingleOrphan.find { it == replyToSecondReplyToParentComment }
        Assert.assertNotNull(leveledReplyToSecondReplyToParentComment)
        Assert.assertEquals(2, leveledReplyToSecondReplyToParentComment!!.level)
        Assert.assertFalse(leveledReplyToSecondReplyToParentComment.isOrphan)
        Assert.assertFalse(leveledReplyToSecondReplyToParentComment.isNestedOrphan)

        val leveledReplyToReplyToSecondReplyToParentComment = leveledCommentListWithSingleOrphan.find { it == replyToReplyToSecondReplyToParentComment }
        Assert.assertNotNull(leveledReplyToReplyToSecondReplyToParentComment)
        Assert.assertEquals(3, leveledReplyToReplyToSecondReplyToParentComment!!.level)
        Assert.assertFalse(leveledReplyToReplyToSecondReplyToParentComment.isOrphan)
        Assert.assertFalse(leveledReplyToReplyToSecondReplyToParentComment.isNestedOrphan)

        val leveledThirdReplyToParentComment = leveledCommentListWithSingleOrphan.find { it == thirdReplyToParentComment }
        Assert.assertNotNull(leveledThirdReplyToParentComment)
        Assert.assertEquals(1, leveledThirdReplyToParentComment!!.level)
        Assert.assertTrue(leveledThirdReplyToParentComment.isOrphan)
        Assert.assertFalse(leveledThirdReplyToParentComment.isNestedOrphan)

        val leveledSecondIndependentComment = leveledCommentListWithSingleOrphan.find { it == secondIndependentComment }
        Assert.assertNotNull(leveledSecondIndependentComment)
        Assert.assertEquals(0, leveledSecondIndependentComment!!.level)
        Assert.assertFalse(leveledSecondIndependentComment.isOrphan)
        Assert.assertFalse(leveledSecondIndependentComment.isNestedOrphan)
    }


    @Test
    fun `given comment list with multiple orphans createLevelList creates list of comments with correct level variable`() {
        var commentLeveler = ReaderCommentLeveler(listOfComments)
        val leveledCommentListWithoutOrphans = commentLeveler.createLevelList()

        leveledCommentListWithoutOrphans.remove(parentComment)
        leveledCommentListWithoutOrphans.remove(secondReplyToParentComment)

        val leveledParentComment = leveledCommentListWithoutOrphans.find { it == parentComment }
        Assert.assertNull(leveledParentComment)

        val leveledSecondReplyToParentCommentComment = leveledCommentListWithoutOrphans.find { it == secondReplyToParentComment }
        Assert.assertNull(leveledSecondReplyToParentCommentComment)

        commentLeveler = ReaderCommentLeveler(leveledCommentListWithoutOrphans)
        val leveledCommentListWithMultipleOrphans = commentLeveler.createLevelList()

        val leveledFirstIndependentComment = leveledCommentListWithMultipleOrphans.find { it == firstIndependentComment }
        Assert.assertNotNull(leveledFirstIndependentComment)
        Assert.assertEquals(0, leveledFirstIndependentComment!!.level)
        Assert.assertFalse(leveledFirstIndependentComment.isOrphan)
        Assert.assertFalse(leveledFirstIndependentComment.isNestedOrphan)

        val leveledReplyToParentCommentComment = leveledCommentListWithMultipleOrphans.find { it == replyToParentComment }
        Assert.assertNotNull(leveledReplyToParentCommentComment)
        Assert.assertEquals(1, leveledReplyToParentCommentComment!!.level)
        Assert.assertTrue(leveledReplyToParentCommentComment.isOrphan)
        Assert.assertFalse(leveledReplyToParentCommentComment.isNestedOrphan)

        val leveledReplyToSecondReplyToParentComment = leveledCommentListWithMultipleOrphans.find { it == replyToSecondReplyToParentComment }
        Assert.assertNotNull(leveledReplyToSecondReplyToParentComment)
        Assert.assertEquals(1, leveledReplyToSecondReplyToParentComment!!.level)
        Assert.assertTrue(leveledReplyToSecondReplyToParentComment.isOrphan)
        Assert.assertFalse(leveledReplyToSecondReplyToParentComment.isNestedOrphan)

        val leveledReplyToReplyToSecondReplyToParentComment = leveledCommentListWithMultipleOrphans.find { it == replyToReplyToSecondReplyToParentComment }
        Assert.assertNotNull(leveledReplyToReplyToSecondReplyToParentComment)
        Assert.assertEquals(2, leveledReplyToReplyToSecondReplyToParentComment!!.level)
        Assert.assertFalse(leveledReplyToReplyToSecondReplyToParentComment.isOrphan)
        Assert.assertFalse(leveledReplyToReplyToSecondReplyToParentComment.isNestedOrphan)

        val leveledThirdReplyToParentComment = leveledCommentListWithMultipleOrphans.find { it == thirdReplyToParentComment }
        Assert.assertNotNull(leveledThirdReplyToParentComment)
        Assert.assertEquals(1, leveledThirdReplyToParentComment!!.level)
        Assert.assertTrue(leveledThirdReplyToParentComment.isOrphan)
        Assert.assertTrue(leveledThirdReplyToParentComment.isNestedOrphan)

        val leveledSecondIndependentComment = leveledCommentListWithMultipleOrphans.find { it == secondIndependentComment }
        Assert.assertNotNull(leveledSecondIndependentComment)
        Assert.assertEquals(0, leveledSecondIndependentComment!!.level)
        Assert.assertFalse(leveledSecondIndependentComment.isOrphan)
        Assert.assertFalse(leveledSecondIndependentComment.isNestedOrphan)
    }

    private fun generateDummyComment(remoteCommentId: Long, parentId: Long) =
            ReaderComment().apply {
                this.commentId = remoteCommentId
                this.parentId = parentId
            }

}
