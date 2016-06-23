package org.wordpress.android.ui.comments;

import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.CommentStatus;

class CommentEvents {

    public static class CommentsBatchModerationFinishedEvent {
        private CommentList mComments;
        private boolean mIsDeleted;
        private CommentStatus mNewStatus;

        public CommentsBatchModerationFinishedEvent(CommentList comments, boolean isDeleted, CommentStatus newStatus) {
            mComments = comments;
            mIsDeleted = isDeleted;
            mNewStatus = newStatus;
        }

        public CommentList getComments() {
            return mComments;
        }

        public boolean isDeleted() {
            return mIsDeleted;
        }

        public CommentStatus getNewStatus() {
            return mNewStatus;
        }

    }

    public static class CommentModerationFinishedEvent {
        private boolean mIsSuccess;
        private boolean mIsCommentsRefreshRequired;
        private long mCommentId;
        private CommentStatus mNewStatus;

        public CommentModerationFinishedEvent(boolean isSuccess, boolean isCommentsRefreshRequired, long commentId, CommentStatus newStatus) {
            mIsSuccess = isSuccess;
            mIsCommentsRefreshRequired = isCommentsRefreshRequired;
            mCommentId = commentId;
            mNewStatus = newStatus;
        }

        public boolean isSuccess() {
            return mIsSuccess;
        }

        public boolean isCommentsRefreshRequired() {
            return mIsCommentsRefreshRequired;
        }

        public long getCommentId() {
            return mCommentId;
        }

        public CommentStatus getNewStatus() {
            return mNewStatus;
        }
    }
}
