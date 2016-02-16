package org.wordpress.android.models;

import android.support.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum CommentStatus implements FilterCriteria {
    UNKNOWN(R.string.comment_status_all),
    UNAPPROVED(R.string.comment_status_unapproved),
    APPROVED(R.string.comment_status_approved),
    TRASH(R.string.comment_status_trash),
    SPAM(R.string.comment_status_spam),
    DELETE(R.string.comment_status_trash);

    private final int mLabelResId;

    CommentStatus(@StringRes int labelResId) {
        mLabelResId = labelResId;
    }

    @Override
    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }

    /*
     * returns the string representation of the passed status, as used by the XMLRPC API
     */
    public static String toString(CommentStatus status) {
        if (status == null){
            return "";
        }

        switch (status) {
            case UNAPPROVED:
                return "hold";
            case APPROVED:
                return "approve";
            case SPAM:
                return "spam";
            case TRASH:
                return "trash";
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
                return "all";
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