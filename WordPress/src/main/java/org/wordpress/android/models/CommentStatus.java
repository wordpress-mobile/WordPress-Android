package org.wordpress.android.models;

public enum CommentStatus {
    UNKNOWN,
    UNAPPROVED,
    APPROVED,
    TRASH,  // <-- REST only
    SPAM;

    /*
     * returns the string representation of the passed status, as used by the XMLRPC API
     */
    public static String toString(CommentStatus status) {
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
    }

    /*
     * returns the string representation of the passed status, as used by the REST API
     */
    public static String toRESTString(CommentStatus status) {
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
    }

    /*
     * returns the status associated with the passed strings - handles both XMLRPC and REST
     */
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
}