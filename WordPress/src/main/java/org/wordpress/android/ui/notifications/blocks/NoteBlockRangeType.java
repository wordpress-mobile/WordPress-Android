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
    UNKNOWN;

    public static NoteBlockRangeType fromString(String value) {
        if (TextUtils.isEmpty(value)) return UNKNOWN;

        if (value.equals("post")) {
            return POST;
        } else if (value.equals("site")) {
            return SITE;
        } else if (value.equals("comment")) {
            return COMMENT;
        } else if (value.equals("user")) {
            return USER;
        } else if (value.equals("stat")) {
            return STAT;
        } else if (value.equals("blockquote")) {
            return BLOCKQUOTE;
        } else if (value.equals("follow")) {
            return FOLLOW;
        } else if (value.equals("noticon")) {
            return NOTICON;
        } else {
            return UNKNOWN;
        }
    }
}
