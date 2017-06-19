package org.wordpress.android.ui.posts.services;

import org.wordpress.android.util.StringUtils;

public class PostEvents {

    public static class PostUploadStarted {
        public final int mLocalBlogId;

        PostUploadStarted(int localBlogId) {
            mLocalBlogId = localBlogId;
        }
    }

    public static class PostMediaInfoUpdated {
        private long mMediaId;
        private String mMediaUrl;

        PostMediaInfoUpdated(long mediaId, String mediaUrl) {
            mMediaId = mediaId;
            mMediaUrl = mediaUrl;
        }
        public long getMediaId() {
            return mMediaId;
        }
        public String getMediaUrl() {
            return StringUtils.notNullStr(mMediaUrl);
        }
    }

    public static class PostMediaCanceled {
        public int localMediaId;
        public boolean delete;
        public boolean all;

        public PostMediaCanceled(int localMediaId, boolean delete) {
            this.localMediaId = localMediaId;
            this.delete = delete;
            this.all = false;
        }
        public PostMediaCanceled(boolean all) {
            this.all = all;
        }
    }
}
