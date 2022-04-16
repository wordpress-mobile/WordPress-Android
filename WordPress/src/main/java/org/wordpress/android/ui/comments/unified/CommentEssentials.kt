package org.wordpress.android.ui.comments.unified

data class CommentEssentials(
    val commentId: Long = DEFAULT_COMMENT_ID,
    val userName: String = "",
    val commentText: String = "",
    val userUrl: String = "",
    val userEmail: String = "",
    val isFromRegisteredUser: Boolean = true
) {
    /**
     * Checks if this instance of CommentEssentials is valid. An invalid instance should not be used to display data.
     * @return true if the instance is valid or false if it's not
     */
    fun isValid(): Boolean = commentId > DEFAULT_COMMENT_ID

    companion object {
        private const val DEFAULT_COMMENT_ID = -1L
    }
}
