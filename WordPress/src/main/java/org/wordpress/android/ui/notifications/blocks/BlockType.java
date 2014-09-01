package org.wordpress.android.ui.notifications.blocks;

/** BlockTypes that we know about
 * Unknown blocks will still be displayed using the rules for BASIC blocks
 */
public enum BlockType {
    UNKNOWN,
    BASIC,
    USER,
    USER_HEADER,
    USER_COMMENT
}
