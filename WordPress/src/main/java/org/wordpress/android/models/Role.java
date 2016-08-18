package org.wordpress.android.models;

import android.support.annotation.Nullable;
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

    public String getDisplayString() {
        return WordPress.getContext().getString(mLabelResId);
    }

    /**
     * @return the string representation of the role, as used by the REST API
     */
    @Nullable
    public String getRESTString() {
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
        return null;
    }

    @Nullable
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
        }
        return null;
    }
}
