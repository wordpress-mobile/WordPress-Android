package org.wordpress.android.ui.reader;


public class ReaderTypes {

    public static final ReaderPostListType DEFAULT_POST_LIST_TYPE = ReaderPostListType.TAG_FOLLOWED;

    public enum ReaderPostListType {
        TAG_FOLLOWED,   // list posts in a followed tag
        TAG_PREVIEW,    // list posts in a specific tag
        BLOG_PREVIEW,   // list posts in a specific blog/feed
        SEARCH_RESULTS; // list posts matching a specific search keyword or phrase

        public boolean isTagType() {
            return this.equals(TAG_FOLLOWED) || this.equals(TAG_PREVIEW);
        }
    }
}
