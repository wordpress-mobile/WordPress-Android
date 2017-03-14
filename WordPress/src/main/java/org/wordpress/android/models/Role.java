package org.wordpress.android.models;

import android.support.annotation.StringRes;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CrashlyticsUtils;

public enum Role {
    ADMIN(R.string.role_admin),
    EDITOR(R.string.role_editor),
    AUTHOR(R.string.role_author),
    CONTRIBUTOR(R.string.role_contributor),
    FOLLOWER(R.string.role_follower),
    VIEWER(R.string.role_viewer),
    SUBSCRIBER(R.string.role_subscriber); // Jetpack only

    private static final Role[] USER_ROLES_WPCOM = { ADMIN, EDITOR, AUTHOR, CONTRIBUTOR };
    private static final Role[] USER_ROLES_JETPACK = { ADMIN, EDITOR, AUTHOR, CONTRIBUTOR, SUBSCRIBER };
    private static final Role[] INVITE_ROLES_WPCOM = { FOLLOWER, ADMIN, EDITOR, AUTHOR, CONTRIBUTOR };
    private static final Role[] INVITE_ROLES_WPCOM_PRIVATE = { VIEWER, ADMIN, EDITOR, AUTHOR, CONTRIBUTOR };
    private static final Role[] INVITE_ROLES_JETPACK = { FOLLOWER };

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
            case "subscriber":
                return SUBSCRIBER;
        }
        Exception e = new IllegalArgumentException("All roles must be handled: " + role);
        CrashlyticsUtils.logException(e, AppLog.T.PEOPLE);

        // All roles should have been handled, but in case an edge case occurs,
        // using "Contributor" role is the safest option
        return CONTRIBUTOR;
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
            case SUBSCRIBER:
                return "subscriber";
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
            case SUBSCRIBER:
                return "subscriber";
        }
        throw new IllegalArgumentException("All roles must be handled");
    }

    public static Role[] userRoles(SiteModel site) {
        if (site.isJetpackConnected()) {
            return USER_ROLES_JETPACK;
        }
        return USER_ROLES_WPCOM;
    }

    public static Role[] inviteRoles(SiteModel site) {
        if (site.isJetpackConnected()) {
            return INVITE_ROLES_JETPACK;
        }

        if (site.isPrivate()) {
            return INVITE_ROLES_WPCOM_PRIVATE;
        }
        return INVITE_ROLES_WPCOM;
    }
}
