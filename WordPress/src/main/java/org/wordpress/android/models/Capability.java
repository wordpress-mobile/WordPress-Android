package org.wordpress.android.models;

/**
 * Used to decide what can the current user do in a particular blog
 * A list of capabilities can be found in: https://codex.wordpress.org/Roles_and_Capabilities#Capabilities
 */
public enum Capability {
    PROMOTE_USERS("promote_users"), // Check if user can change another user's role
    LIST_USERS("list_users"); // Check if user can visit People page

    private final String label;

    Capability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
