package org.wordpress.android.ui.notifications.blocks

import android.text.TextUtils

/**
 * BlockTypes that we know about
 * Unknown blocks will still be displayed using the rules for BASIC blocks
 */
enum class BlockType {
    UNKNOWN,
    BASIC,
    USER,
    USER_HEADER,
    USER_COMMENT,
    FOOTER;

    companion object {
        fun fromString(blockType: String?): BlockType {
            return if (TextUtils.isEmpty(blockType)) {
                UNKNOWN
            } else when (blockType) {
                "basic" -> BASIC
                "user" -> USER
                "user_header" -> USER_HEADER
                "user_comment" -> USER_COMMENT
                "footer" -> FOOTER
                else -> UNKNOWN
            }
        }
    }
}
