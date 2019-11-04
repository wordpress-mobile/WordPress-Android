package org.wordpress.android.ui.uploads;

import org.wordpress.android.fluxc.model.PostImmutableModel;

public class PostEvents {
    public static class PostUploadStarted {
        public final PostImmutableModel post;

        public PostUploadStarted(PostImmutableModel post) {
            this.post = post;
        }
    }

    public static class PostUploadCanceled {
        public final PostImmutableModel post;

        PostUploadCanceled(PostImmutableModel post) {
            this.post = post;
        }
    }

    public static class PostMediaCanceled {
        public PostImmutableModel post;

        public PostMediaCanceled(PostImmutableModel post) {
            this.post = post;
        }
    }

    public static class PostOpenedInEditor {
        public final int localSiteId;
        public final int postId;

        public PostOpenedInEditor(int localSiteId, int postId) {
            this.localSiteId = localSiteId;
            this.postId = postId;
        }
    }

    public static class PostPreviewingInEditor {
        public final int localSiteId;
        public final int postId;

        public PostPreviewingInEditor(int localSiteId, int postId) {
            this.localSiteId = localSiteId;
            this.postId = postId;
        }
    }
}
