package org.wordpress.android.fluxc.model.post;

import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.util.DateTimeUtils;

import java.util.Date;
import java.util.List;

public enum PostStatus {
    UNKNOWN,
    PUBLISHED,
    DRAFT,
    PRIVATE,
    PENDING,
    TRASHED,
    SCHEDULED; // NOTE: Only recognized for .com REST posts - XML-RPC returns scheduled posts with status 'publish'

    public String toString() {
        switch (this) {
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

    public static synchronized PostStatus fromPost(PostImmutableModel post) {
        String value = post.getStatus();
        long dateCreatedGMT = 0;

        Date dateCreated = DateTimeUtils.dateUTCFromIso8601(post.getDateCreated());
        if (dateCreated != null) {
            dateCreatedGMT = dateCreated.getTime();
        }

        return fromStringAndDateGMT(value, dateCreatedGMT);
    }

    public static String postStatusListToString(List<PostStatus> statusList) {
        String statusString = "";
        boolean firstTime = true;

        for (PostStatus postStatus : statusList) {
            if (firstTime) {
                firstTime = false;
            } else {
                statusString += ",";
            }
            statusString += postStatus.toString();
        }

        return statusString;
    }
}
