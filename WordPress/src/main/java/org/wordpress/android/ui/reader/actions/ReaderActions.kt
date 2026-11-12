package org.wordpress.android.ui.reader.actions

/**
 * Listener and result types shared by the Reader's network requests
 */
object ReaderActions {
    /*
     * listener when the failure status code is required
     */
    interface OnRequestListener<T> {
        fun onSuccess(result: T?)

        fun onFailure(statusCode: Int)
    }

    /*
     * result when updating data (getting latest posts or comments for a post, etc.)
     */
    enum class UpdateResult {
        HAS_NEW, // new posts/comments/etc. have been retrieved
        CHANGED, // no new posts/comments, but existing ones have changed
        UNCHANGED, // no new or changed posts/comments
        FAILED; // request failed

        val isNewOrChanged: Boolean
            get() = this == HAS_NEW || this == CHANGED
    }

    fun interface UpdateResultListener {
        fun onUpdateResult(result: UpdateResult)
    }
}
