package org.wordpress.android.ui.comments;

import org.wordpress.android.models.CommentList;

class CommentEvents {

    public static class CommentsBatchModerationFinishedEvent {
        private CommentList mComments;
        private boolean mIsDeleted;

        public CommentsBatchModerationFinishedEvent(CommentList comments, boolean isDeleted) {
            mComments = comments;
            mIsDeleted = isDeleted;
        }

        public CommentList getComments() {
            return mComments;
        }

        public boolean isDeleted() {
            return mIsDeleted;
        }
    }


    public static class CommentModerationFinishedEvent {
        private boolean mIsSuccess;
        private boolean mIsCommentsRefreshRequired;
        private long mCommentId;

        public CommentModerationFinishedEvent(boolean isSuccess, boolean isCommentsRefreshRequired, long commentId) {
            mIsSuccess = isSuccess;
            mIsCommentsRefreshRequired = isCommentsRefreshRequired;
            mCommentId = commentId;
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
    }
}
