package org.wordpress.android.models;

/**
 * Created by nbradbury on 11/14/13.
 */
public enum CommentStatus {
    UNKNOWN (1),
    APPROVED (2),
    UNAPPROVED (3),
    SPAM (4),
    TRASH (5);

    public static enum ApiFormat {XMLRPC, REST}
    private static int[] sSelectedCommentStatusTypeCountArray = new int[CommentStatus.values().length];
    private final int mOffset;

    CommentStatus(int offset) { mOffset = offset; }

    public int getOffset() { return mOffset; }

    public static void incrementSelectedCommentStatusTypeCount(CommentStatus commentStatus) {
        sSelectedCommentStatusTypeCountArray[commentStatus.ordinal()] += 1;
    }

    public static int getSelectedCommentStatusTypeCount(CommentStatus commentStatus) {
        return sSelectedCommentStatusTypeCountArray[commentStatus.ordinal()];
    }

    public static void clearSelectedCommentStatusTypeCount() {
        for(int i=0; i<sSelectedCommentStatusTypeCountArray.length; i++) {
            sSelectedCommentStatusTypeCountArray[i] = 0;
        }
    }

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

        /* for future reference, REST API uses these strings:
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
        } */
    };

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
};