package org.wordpress.android.fluxc.model.post;

import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.utils.DateTimeUtils;

import java.util.Date;

public enum PostStatus {
    UNKNOWN,
    PUBLISHED,
    DRAFT,
    PRIVATE,
    PENDING,
    TRASHED,
    SCHEDULED; // NOTE: Only used locally, WP has a 'future' status but it is not returned from the metaWeblog API

    private static synchronized PostStatus fromStringAndDateGMT(String value, long dateCreatedGMT) {
        if (value == null) {
            return UNKNOWN;
        } else if (value.equals("publish")) {
            // Check if post is scheduled
            Date d = new Date();
            // Subtract 10 seconds from the server GMT date, in case server and device time slightly differ
            if (dateCreatedGMT - 10000 > d.getTime()) {
                return SCHEDULED;
            }
            return PUBLISHED;
        } else if (value.equals("draft")) {
            return DRAFT;
        } else if (value.equals("private")) {
            return PRIVATE;
        } else if (value.equals("pending")) {
            return PENDING;
        } else if (value.equals("trash")) {
            return TRASHED;
        } else if (value.equals("future")) {
            return SCHEDULED;
        } else {
            return UNKNOWN;
        }
    }

    public static synchronized PostStatus fromPost(PostModel post) {
        String value = post.getStatus();
        long dateCreatedGMT = 0;
        if (post.getDateCreated() != null) {
            Date dateCreated = DateTimeUtils.dateFromIso8601(post.getDateCreated());
            if (dateCreated != null) {
                dateCreatedGMT = dateCreated.getTime();
            }
        }
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
