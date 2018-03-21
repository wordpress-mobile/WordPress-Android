package org.wordpress.android.models;

/**
 * Used to decide what can the current user do in a particular blog
 * A list of capabilities can be found in: https://codex.wordpress.org/Roles_and_Capabilities#Capabilities
 */
public enum Capability {
    LIST_USERS("list_users"), // Check if user can visit People page
    PROMOTE_USERS("promote_users"), // Check if user can change another user's role
    REMOVE_USERS("remove_users"), // Check if user can remove another user
    EDIT_PAGES("edit_pages"); // Check if user can edit Pages

    private final String mLabel;

    Capability(String label) {
        this.mLabel = label;
    }

    public String getLabel() {
        return mLabel;
    }
}
