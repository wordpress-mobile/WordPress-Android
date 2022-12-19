package org.wordpress.android.ui.comments.unified.usecase

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.store.CommentsStore
import javax.inject.Inject

class GetCommentUseCase @Inject constructor(
    private val commentsStore: CommentsStore
) {
    suspend fun execute(siteModel: SiteModel, remoteCommentId: Long): CommentEntity? {
        val localCommentEntityList =
                commentsStore.getCommentByLocalSiteAndRemoteId(siteModel.id, remoteCommentId)
        return if (localCommentEntityList.isNullOrEmpty()) {
            val commentResponse = commentsStore.fetchComment(siteModel, remoteCommentId, null)
            val remoteCommentEntityList = commentResponse.data?.comments
            remoteCommentEntityList?.firstOrNull()
        } else {
            localCommentEntityList.firstOrNull()
        }
    }
}
