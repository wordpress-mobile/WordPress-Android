package org.wordpress.android.models;

/**
 * Created by roundhill on 3/4/14.
 */
public enum PostStatus {
    UNKNOWN,
    DRAFT,
    PRIVATE,
    PENDING,
    SCHEDULED, //NOTE: Only used locally, WP has a 'future' status but it is not returned from the metaWeblog API
    PUBLISHED;

    public static PostStatus fromString(String value) {
        if (value == null)
            return PostStatus.UNKNOWN;
        if (value.equals("draft"))
            return PostStatus.DRAFT;
        if (value.equals("private"))
            return PostStatus.PRIVATE;
        if (value.equals("pending"))
            return PENDING;
        if (value.equals("future"))
            return SCHEDULED;
        if (value.equals("publish"))
            return PUBLISHED;

        return PostStatus.UNKNOWN;
    }

    public static String toString(PostStatus status) {
        switch (status) {
            case DRAFT:
                return "draft";
            case PRIVATE:
                return "private";
            case PENDING:
                return "pending";
            case SCHEDULED:
                return "future";
            case PUBLISHED:
                return "publish";
            default:
                return "";
        }
    }
}
