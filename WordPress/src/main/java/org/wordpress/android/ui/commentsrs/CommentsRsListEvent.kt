package org.wordpress.android.ui.commentsrs

import androidx.annotation.StringRes
import org.wordpress.android.fluxc.model.SiteModel

sealed interface CommentsRsListEvent {
    data class OpenCommentDetail(val site: SiteModel, val remoteCommentId: Long) : CommentsRsListEvent
    data class ShowToast(@StringRes val messageResId: Int) : CommentsRsListEvent
    object Finish : CommentsRsListEvent
}
