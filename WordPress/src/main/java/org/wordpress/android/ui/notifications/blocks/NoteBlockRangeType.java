package org.wordpress.android.ui.notifications.blocks;

import android.text.TextUtils;

/**
 * Known NoteBlock Range types
 */
public enum NoteBlockRangeType {
    POST,
    SITE,
    COMMENT,
    USER,
    STAT,
    BLOCKQUOTE,
    FOLLOW,
    NOTICON,
    LIKE,
    MATCH,
    UNKNOWN;

    public static NoteBlockRangeType fromString(String value) {
        if (TextUtils.isEmpty(value)) return UNKNOWN;

        switch (value) {
            case "post":
                return POST;
            case "site":
                return SITE;
            case "comment":
                return COMMENT;
            case "user":
                return USER;
            case "stat":
                return STAT;
            case "blockquote":
                return BLOCKQUOTE;
            case "follow":
                return FOLLOW;
            case "noticon":
                return NOTICON;
            case "like":
                return LIKE;
            case "match":
                return MATCH;
            default:
                return UNKNOWN;
        }
    }
}
