package org.wordpress.android.ui.posts.services;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.util.StringUtils;

public class PostEvents {

    public static class PostUploadStarted {
        public final int mLocalBlogId;

        PostUploadStarted(int localBlogId) {
            mLocalBlogId = localBlogId;
        }
    }

    public static class PostUploadCanceled {
        public final int localSiteId;

        public PostUploadCanceled(int localSiteId) {
            this.localSiteId = localSiteId;
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
        public PostModel post;

        public PostMediaCanceled(PostModel post) {
            this.post = post;
        }
    }
}
