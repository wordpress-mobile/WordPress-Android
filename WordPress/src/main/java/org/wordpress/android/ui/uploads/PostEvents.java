package org.wordpress.android.ui.uploads;

import org.wordpress.android.fluxc.model.PostModel;

public class PostEvents {
    public static class PostUploadStarted {
        public final PostModel post;

        PostUploadStarted(PostModel post) {
            this.post = post;
        }
    }

    public static class PostUploadCanceled {
        public final PostModel post;

        PostUploadCanceled(PostModel post) {
            this.post = post;
        }
    }

    public static class PostMediaCanceled {
        public PostModel post;

        public PostMediaCanceled(PostModel post) {
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
}
