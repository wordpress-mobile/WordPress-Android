package org.wordpress.android.ui.reader;

import android.support.annotation.NonNull;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.StringUtils;

/**
 * Reader-related EventBus event classes
 */
public class ReaderEvents {
    public static class FollowedTagsChanged {}
    public static class RecommendedTagsChanged{}

    public static class FollowedBlogsChanged {}
    public static class RecommendedBlogsChanged {}

    public static class PostSelected {
        private ReaderPost mPost;
        public PostSelected(@NonNull ReaderPost post) {
            mPost = post;
        }
        public ReaderPost getPost() {
            return mPost;
        }
    }

    public static class TagSelected {
        private String mTagName;
        public TagSelected(String tagName) {
            mTagName = StringUtils.notNullStr(tagName);
        }
        public String getTagName() {
            return mTagName;
        }
    }

    public static class ReblogRequested {
        private ReaderPost mPost;
        public ReblogRequested(@NonNull ReaderPost post) {
            mPost = post;
        }
        public ReaderPost getPost() {
            return mPost;
        }
    }
}
