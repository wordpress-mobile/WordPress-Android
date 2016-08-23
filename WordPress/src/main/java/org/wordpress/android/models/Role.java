package org.wordpress.android.models;

import android.support.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum Role {
    ADMIN(R.string.role_admin),
    EDITOR(R.string.role_editor),
    AUTHOR(R.string.role_author),
    CONTRIBUTOR(R.string.role_contributor),
    FOLLOWER(R.string.role_follower),
    VIEWER(R.string.role_viewer);

    private final int mLabelResId;

    Role(@StringRes int labelResId) {
        mLabelResId = labelResId;
    }

    public String toDisplayString() {
        return WordPress.getContext().getString(mLabelResId);
    }

    public static Role fromString(String role) {
        switch (role) {
            case "administrator":
                return ADMIN;
            case "editor":
                return EDITOR;
            case "author":
                return AUTHOR;
            case "contributor":
                return CONTRIBUTOR;
            case "follower":
                return FOLLOWER;
            case "viewer":
                return VIEWER;
        }
        throw new IllegalArgumentException("All roles must be handled");
    }

    @Override
    public String toString() {
        switch (this) {
            case ADMIN:
                return "administrator";
            case EDITOR:
                return "editor";
            case AUTHOR:
                return "author";
            case CONTRIBUTOR:
                return "contributor";
            case FOLLOWER:
                return "follower";
            case VIEWER:
                return "viewer";
        }
        throw new IllegalArgumentException("All roles must be handled");
    }

    /**
     * @return the string representation of the role, as used by the REST API
     */
    public String toRESTString() {
        switch (this) {
            case ADMIN:
                return "administrator";
            case EDITOR:
                return "editor";
            case AUTHOR:
                return "author";
            case CONTRIBUTOR:
                return "contributor";
            case FOLLOWER:
                return "follower";
            case VIEWER:
                // the remote expects "follower" as the role parameter even if the role is "viewer"
                return "follower";
        }
        throw new IllegalArgumentException("All roles must be handled");
    }

    public static Role[] userRoles() {
        return new Role[] { ADMIN, EDITOR, AUTHOR, CONTRIBUTOR };
    }

    public static Role[] inviteRoles(boolean isPrivateSite) {
        if (isPrivateSite) {
            return new Role[] { VIEWER, ADMIN, EDITOR, AUTHOR, CONTRIBUTOR };
        }
        return new Role[] { FOLLOWER, ADMIN, EDITOR, AUTHOR, CONTRIBUTOR };
    }
}
