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
import java.util.Random

@RunWith(RobolectricTestRunner::class)
class CommentSqlUtilsTest {
    private val mRandom = Random(System.currentTimeMillis())

    val site = SiteModel()
    @Before fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config: WellSqlConfig = SingleStoreWellSqlConfigForTests(appContext, CommentModel::class.java)
        WellSql.init(config)
        config.reset()

        site.id = 1
        site.siteId = 2
    }

    // Attempts to insert null then verifies there is no media
    @Test fun `removeDeletedComments correctly removes deleted comments with mixed statuses from mixed list`() {
        val commentsInDb = generateCommentModels(40, ALL)
        val freshComments = generateCommentModels(33, ALL)
        freshComments.removeIf { it.remoteCommentId == 5L }
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
                .isEqualTo(40)

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(site, freshComments, false, false, APPROVED, UNAPPROVED)
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                APPROVED,
                UNAPPROVED
        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(3)
        Assertions.assertThat(cleanedComments.size).isEqualTo(37)
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 5L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 15L }).isNull()
        Assertions.assertThat(cleanedComments.find { it.remoteCommentId == 20L }).isNull()
    }

    // Attempts to insert null then verifies there is no media
    @Test fun `removeDeletedComments trims deleted comments from bottom of DB when reaching the end of dataset`() {
        val commentsInDb = generateCommentModels(50, ALL)
        val freshComments = generateCommentModels(29, ALL, 31)

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

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(
                site,
                freshComments,
                true,
                false,
                APPROVED,
                UNAPPROVED
        )
        val cleanedComments = CommentSqlUtils.getCommentsForSite(
                site,
                SelectQuery.ORDER_DESCENDING,
                APPROVED,
                UNAPPROVED
        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(20)
        Assertions.assertThat(cleanedComments.size).isEqualTo(30)
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
                .isEqualTo(59)
    }


    // Attempts to insert null then verifies there is no media
    @Test fun `removeDeletedComments trims deleted comments from the top of DB when begining of dataset excludes them`() {
        val commentsInDb = generateCommentModels(50, ALL)
        val freshComments = generateCommentModels(30, ALL, 3)

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

        val numCommentsDeleted = CommentSqlUtils.removeDeletedComments(site, freshComments, false, true, ALL)
//        val cleanedComments = CommentSqlUtils.getCommentsForSite(
//                site,
//                SelectQuery.ORDER_DESCENDING,
//                APPROVED,
//                UNAPPROVED
//        )

        Assertions.assertThat(numCommentsDeleted).isEqualTo(3)
//        Assertions.assertThat(cleanedComments.size).isEqualTo(30)
//        freshComments.forEach {
//            CommentSqlUtils.insertOrUpdateComment(it)
//        }
//
//        Assertions.assertThat(
//                CommentSqlUtils.getCommentsForSite(
//                        site,
//                        SelectQuery.ORDER_DESCENDING,
//                        APPROVED,
//                        UNAPPROVED
//                ).size
//        )
//                .isEqualTo(59)
    }

    private fun generateCommentModels(num: Int, status: CommentStatus, startId: Int = 1): ArrayList<CommentModel> {
        val commentModels = ArrayList<CommentModel>()
        for (i in 0 until num) {
            val comment = CommentModel()
            comment.remoteCommentId = startId + i.toLong()
            comment.publishedTimestamp = startId + i.toLong()
            comment.localSiteId = 1
            comment.remoteSiteId = 2
            if (status == ALL) {
                comment.status = if (i % 2 == 0) APPROVED.toString() else CommentStatus.UNAPPROVED.toString()
            } else {
                comment.status = status.toString()
            }
            commentModels.add(comment)
        }
        return commentModels
    }
}
