package org.wordpress.android.ui.reader;


public class ReaderTypes {

    public static final ReaderPostListType DEFAULT_POST_LIST_TYPE = ReaderPostListType.TAG_FOLLOWED;

    public static enum ReaderFragmentType {POST_LIST, POST_DETAIL}

    public static enum ReaderPostListType {
        TAG_FOLLOWED,   // list posts in a followed tag
        TAG_PREVIEW,    // list posts in a specific tag
        BLOG_PREVIEW;   // list posts in a specific blog

        public boolean isTagType() {
            return this.equals(TAG_FOLLOWED) || this.equals(TAG_PREVIEW);
        }

        public boolean isPreviewType() {
            return this.equals(TAG_PREVIEW) || this.equals(BLOG_PREVIEW);
        }
    }

    protected static enum PostChangeType {LIKED, UNLIKED, FOLLOWED, UNFOLLOWED, CONTENT}

    protected static enum RefreshType {AUTOMATIC, MANUAL}
}
