package org.wordpress.android.models;

import java.util.Date;

public enum PostStatus {
    UNKNOWN,
    PUBLISHED,
    DRAFT,
    PRIVATE,
    PENDING,
    TRASHED,
    SCHEDULED; //NOTE: Only used locally, WP has a 'future' status but it is not returned from the metaWeblog API

    private synchronized static PostStatus fromStringAndDateGMT(String value, long dateCreatedGMT) {
        if (value == null) {
            return PostStatus.UNKNOWN;
        } else if (value.equals("publish")) {
            // Check if post is scheduled
            Date d = new Date();
            // Subtract 10 seconds from the server GMT date, in case server and device time slightly differ
            if (dateCreatedGMT - 10000 > d.getTime()) {
                return SCHEDULED;
            }
            return PUBLISHED;
        } else if (value.equals("draft")) {
            return PostStatus.DRAFT;
        } else if (value.equals("private")) {
            return PostStatus.PRIVATE;
        } else if (value.equals("pending")) {
            return PENDING;
        } else if (value.equals("trash")) {
            return TRASHED;
        } else if (value.equals("future")) {
            return SCHEDULED;
        } else {
            return PostStatus.UNKNOWN;
        }
    }

    public synchronized static PostStatus fromPost(Post post) {
        String value = post.getPostStatus();
        long dateCreatedGMT = post.getDate_created_gmt();
        return fromStringAndDateGMT(value, dateCreatedGMT);
    }

    public synchronized static PostStatus fromPostsListPost(PostsListPost post) {
        String value = post.getOriginalStatus();
        long dateCreatedGMT = post.getDateCreatedGmt();
        return fromStringAndDateGMT(value, dateCreatedGMT);
    }

    public static String toString(PostStatus status) {
        switch (status) {
            case PUBLISHED:
                return "publish";
            case DRAFT:
                return "draft";
            case PRIVATE:
                return "private";
            case PENDING:
                return "pending";
            case TRASHED:
                return "trash";
            case SCHEDULED:
                return "future";
            default:
                return "";
        }
    }
}
