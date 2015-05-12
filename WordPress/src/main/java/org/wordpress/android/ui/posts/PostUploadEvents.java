package org.wordpress.android.ui.posts;

public class PostUploadEvents {
    public static class PostUploadSucceed {
        public final int mLocalBlogId;
        public final String mRemotePostId;
        public final boolean mIsPage;

        PostUploadSucceed(int localBlogId, String remotePostId, boolean isPage) {
            mLocalBlogId = localBlogId;
            mRemotePostId = remotePostId;
            mIsPage = isPage;
        }
    }

    public static class PostUploadFailed {
        public final int mLocalId;

        PostUploadFailed(int localId) {
            mLocalId = localId;
        }
    }
}
