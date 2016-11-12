package org.wordpress.android.ui.notifications.blocks;

import android.text.TextUtils;

/** BlockTypes that we know about
 * Unknown blocks will still be displayed using the rules for BASIC blocks
 */
public enum BlockType {
    UNKNOWN,
    BASIC,
    USER,
    USER_HEADER,
    USER_COMMENT,
    FOOTER;

    public static BlockType fromString(String blockType) {
        if (TextUtils.isEmpty(blockType)) return UNKNOWN;

        switch (blockType) {
            case "basic":
                return BASIC;
            case "user":
                return USER;
            case "user_header":
                return USER_HEADER;
            case "user_comment":
                return USER_COMMENT;
            case "footer":
                return FOOTER;
            default:
                return UNKNOWN;
        }
    }
}
