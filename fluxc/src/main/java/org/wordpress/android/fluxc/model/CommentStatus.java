package org.wordpress.android.fluxc.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public enum CommentStatus {
    // Real status
    APPROVED,
    UNAPPROVED,
    SPAM,
    TRASH,
    DELETED,

    // Used for filtering
    ALL,
    UNREPLIED,

    // Used for editing
    UNSPAM,  // Unmark the comment as spam. Will attempt to set it to the previous status.
    UNTRASH; // Untrash a comment. Only works when the comment is in the trash.

    public String toString() {
        return this.name().toLowerCase(Locale.US);
    }

    @NonNull
    public static CommentStatus fromString(@Nullable String string) {
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
