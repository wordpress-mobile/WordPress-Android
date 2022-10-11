@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments.unified

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.CommentsDetailActivity
import org.wordpress.android.ui.comments.unified.CommentDetailsActivityContract.CommentDetailsActivityRequest
import org.wordpress.android.ui.comments.unified.CommentDetailsActivityContract.CommentDetailsActivityResponse

class CommentDetailsActivityContract : ActivityResultContract<CommentDetailsActivityRequest,
        CommentDetailsActivityResponse?>() {
    @Suppress("DEPRECATION")
    override fun createIntent(context: Context, input: CommentDetailsActivityRequest): Intent {
        val detailIntent = Intent(context, CommentsDetailActivity::class.java)
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_ID_EXTRA, input.commentId)
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_STATUS_FILTER_EXTRA, input.commentStatus)
        detailIntent.putExtra(WordPress.SITE, input.site)
        return detailIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CommentDetailsActivityResponse? = when {
        resultCode != Activity.RESULT_OK || intent == null -> null
        else -> {
            val commentId = intent.getLongExtra(CommentConstants.COMMENT_MODERATE_ID_EXTRA, -1)
            val newStatus = intent.getStringExtra(CommentConstants.COMMENT_MODERATE_STATUS_EXTRA)
            CommentDetailsActivityResponse(commentId, CommentStatus.fromString(newStatus))
        }
    }

    data class CommentDetailsActivityRequest(
        val commentId: Long,
        val commentStatus: CommentStatus,
        val site: SiteModel
    )

    data class CommentDetailsActivityResponse(val commentId: Long, val commentStatus: CommentStatus)
}
