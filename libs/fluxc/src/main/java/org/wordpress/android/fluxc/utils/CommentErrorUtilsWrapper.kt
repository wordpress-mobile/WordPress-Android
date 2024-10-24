package org.wordpress.android.fluxc.utils

import dagger.Reusable
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import javax.inject.Inject

@Reusable
class CommentErrorUtilsWrapper @Inject constructor() {
    fun networkToCommentError(error: BaseNetworkError): CommentError = CommentErrorUtils.networkToCommentError(error)
}
