package org.wordpress.android.fluxc.comment

import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.ALL
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.CommentSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import java.util.ArrayList

@RunWith(RobolectricTestRunner::class)
class CommentSqlUtilsTest {
    val site = SiteModel().apply {
        id = 1
        siteId = 2
    }

    @Before fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config: WellSqlConfig = SingleStoreWellSqlConfigForTests(appContext, CommentModel::class.java)
        WellSql.init(config)
        config.reset()
    }

    @Test fun `removeDeletedComments correctly removes deleted comments with mixed statuses from mixed list`() {
        val commentsInDb = generateCommentModels(60, ALL) // timestamp from 60 to 1
        val freshComments = generateCommentModels(33, ALL, 28) // timestamp from 60 to 28

        // remove 3 comments from the middle so we will get 30 comments in the list
        freshComments.removeIf { it.remoteCommentId == 55L }
        freshComments.removeIf { it.remoteCommentId == 45L }
        freshComments.removeIf { it.remoteCommentId == 35L }

        commentsInDb.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(60)

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(site, freshComments, 30, 0, APPROVED, UNAPPROVED)
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                APPROVED,
                UNAPPROVED
        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(3)
        Assertions.assertThat(cleanedComments.size).isEqualTo(57)
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 55L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 45L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 35L }).isNull()
    }

    @Test fun `removeDeletedComments correctly removes deleted comments with one status from mixed list`() {
        val commentsInDb = generateCommentModels(60, ALL) // timestamp from 60 to 1

        commentsInDb.find { it.remoteCommentId == 55L }?.status = APPROVED.toString()
        commentsInDb.find { it.remoteCommentId == 45L }?.status = APPROVED.toString()
        commentsInDb.find { it.remoteCommentId == 35L }?.status = APPROVED.toString()

        val freshComments = generateCommentModels(33, APPROVED, 28) // timestamp from 60 to 28
        // remove 3 comments from the middle so we will get 30 comments in the list
        freshComments.removeIf { it.remoteCommentId == 55L }
        freshComments.removeIf { it.remoteCommentId == 45L }
        freshComments.removeIf { it.remoteCommentId == 35L }

        commentsInDb.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(60)

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(site, freshComments, 30, 0, APPROVED)
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                ALL
        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(3)
        Assertions.assertThat(cleanedComments.size).isEqualTo(57)
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 55L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 45L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 35L }).isNull()
    }

    @Test fun `removeDeletedComments removes comments from start, end and the middle of DB`() {
        val commentsInDb = generateCommentModels(65, ALL)
        val freshComments = generateCommentModels(25, ALL, 4)

        // 3 comments are missing from the middle
        freshComments.removeIf { it.remoteCommentId == 7L }
        freshComments.removeIf { it.remoteCommentId == 15L }
        freshComments.removeIf { it.remoteCommentId == 20L }

        commentsInDb.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(65)

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(
                site,
                freshComments,
                30,
                0,
                APPROVED,
                UNAPPROVED
        )
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                APPROVED,
                UNAPPROVED
        )
        // from 65 comments in DB we expect to have only 22 fresh comments (remove first 3, 3 from the middle and all the comments that go after last comment in freshComments
        Assertions.assertThat(numCommentsDeleted).isEqualTo(43)
        Assertions.assertThat(cleanedComments.size).isEqualTo(22)
        freshComments.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 7L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 15L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 20L }).isNull()
    }

    @Test fun `removeDeletedComments trims deleted comments from bottom of DB when reaching the end of dataset`() {
        val commentsInDb = generateCommentModels(60, ALL)
        val freshComments = generateCommentModels(30, ALL)

        freshComments.removeLast()
        freshComments.removeLast()
        freshComments.removeLast()

        commentsInDb.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(60)

        // simulate loading more comments
        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(
                site,
                freshComments,
                30,
                30,
                APPROVED,
                UNAPPROVED
        )
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                APPROVED,
                UNAPPROVED
        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(3) // we remove comment 1,2 and 3
        Assertions.assertThat(cleanedComments.size).isEqualTo(57)
        freshComments.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(57)
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 1L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 2L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 3L }).isNull()
    }

    @Test
    fun `removeDeletedComments trims deleted comments from the top of DB when beginning of dataset excludes them`() {
        val commentsInDb = generateCommentModels(50, ALL)
        val freshComments = generateCommentModels(50, ALL)

        // exclude first 3 comments
        freshComments.removeFirst()
        freshComments.removeFirst()
        freshComments.removeFirst()

        commentsInDb.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(50)

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(site, freshComments, 30, 0, ALL)
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                APPROVED,
                UNAPPROVED
        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(3)
        Assertions.assertThat(cleanedComments.size).isEqualTo(47)
        freshComments.forEach {
            CommentSqlUtils.insertOrUpdateComment(it)
        }

        Assertions.assertThat(
                CommentSqlUtils.getCommentsForSite(
                        site,
                        SelectQuery.ORDER_DESCENDING,
                        APPROVED,
                        UNAPPROVED
                ).size
        )
                .isEqualTo(47)
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 50L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 49L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 48L }).isNull()
    }

    private fun generateCommentModels(num: Int, status: CommentStatus, startId: Int = 1): ArrayList<CommentModel> {
        val commentModels = ArrayList<CommentModel>()
        for (i in 0 until num) {
            val comment = CommentModel()
            comment.remoteCommentId = startId + i.toLong()
            comment.publishedTimestamp = startId + i.toLong()
            comment.localSiteId = site.id
            comment.remoteSiteId = site.siteId
            if (status == ALL) {
                comment.status = if (i % 2 == 0) APPROVED.toString() else UNAPPROVED.toString()
            } else {
                comment.status = status.toString()
            }
            commentModels.add(comment)
        }
        commentModels.reverse() // we usually receive comments starting from more recent
        return commentModels
    }
}
