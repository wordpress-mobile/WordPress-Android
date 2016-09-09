package org.wordpress.android.fluxc.model;

public enum CommentStatus {
    ALL,
    APPROVED,
    UNAPPROVED,
    SPAM,
    TRASH;

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
