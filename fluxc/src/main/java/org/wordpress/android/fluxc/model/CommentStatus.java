package org.wordpress.android.fluxc.model;

public enum CommentStatus {
    // Real status
    APPROVED,
    UNAPPROVED,
    SPAM,
    TRASH,

    // Used for filtering
    ALL,

    // Used for editing
    UNSPAM,  // Unmark the comment as spam. Will attempt to set it to the previous status.
    UNTRASH; // Untrash a comment. Only works when the comment is in the trash.

    public String toString() {
        return this.name().toLowerCase();
    }

    public static CommentStatus fromString(String string) {
        if (string != null) {
            for (CommentStatus v : CommentStatus.values()) {
                if (string.equalsIgnoreCase(v.name())) {
                    return v;
                }
            }
        }
        return ALL;
    }
}
