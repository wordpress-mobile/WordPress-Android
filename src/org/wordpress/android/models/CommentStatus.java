package org.wordpress.android.models;

/**
 * Created by nbradbury on 11/14/13.
 */
public enum CommentStatus {
    UNKNOWN,
    UNAPPROVED,
    APPROVED,
    TRASH,
    SPAM;

    // XMLRPC and REST use different strings - consider XMLRPC the default, since that's the format
    // the app has stored the comment status as since the start
    public static String toString(CommentStatus status, ApiFormat format) {
        switch (format) {
            case XMLRPC:
                switch (status) {
                    case UNAPPROVED:
                        return "hold";
                    case APPROVED:
                        return "approve";
                    case SPAM:
                        return "spam";
                    default:
                        return "";
                }

            case REST:
                switch (status) {
                    case UNAPPROVED:
                        return "unapproved";
                    case APPROVED:
                        return "approved";
                    case SPAM:
                        return "spam";
                    case TRASH:
                        return "trash";
                    default:
                        return "";
                }

            default:
                return "";
        }
    };

    public static CommentStatus fromString(String value) {
        if (value == null)
            return CommentStatus.UNKNOWN;
        if (value.equals("approve") || value.equals("approved"))
            return CommentStatus.APPROVED;
        if (value.equals("hold") || value.equals("unapproved"))
            return CommentStatus.UNAPPROVED;
        if (value.equals("spam"))
            return SPAM;
        if (value.equals("trash"))
            return TRASH;
        return CommentStatus.UNKNOWN;
    }

    public static enum ApiFormat {XMLRPC, REST}
};