package org.wordpress.android.ui.notifications.blocks;

import android.text.TextUtils;

/**
 * Known NoteBlock Id types
 */
public enum NoteBlockIdType {
    POST,
    SITE,
    COMMENT,
    USER,
    UNKNOWN;

    public static NoteBlockIdType fromString(String value) {
        if (TextUtils.isEmpty(value)) return UNKNOWN;

        if (value.equals("post")) {
            return POST;
        } else if (value.equals("site")) {
            return SITE;
        } else if (value.equals("comment")) {
            return COMMENT;
        } else if (value.equals("user")) {
            return USER;
        } else {
            return UNKNOWN;
        }
    }
}
