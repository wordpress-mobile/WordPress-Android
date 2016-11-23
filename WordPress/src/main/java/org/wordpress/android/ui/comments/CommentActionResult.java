package org.wordpress.android.ui.comments;

public class CommentActionResult {

    public static final int COMMENT_ID_ON_ERRORS = -1;
    public static final int COMMENT_ID_UNKNOWN = -2; // This is used primarily for replies, when the commentID isn't known.

    private long mCommentID = COMMENT_ID_UNKNOWN;
    private final String mMessage;

    public CommentActionResult(long commentID, String message) {
        mCommentID = commentID;
        mMessage = message;
    }

    public String getMessage() { return mMessage; }
    public boolean isSuccess() { return mCommentID != COMMENT_ID_ON_ERRORS; }
}
