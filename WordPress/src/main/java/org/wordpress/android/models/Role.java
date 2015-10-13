package org.wordpress.android.models;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import org.wordpress.android.R;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    EDITOR,
    AUTHOR,
    CONTRIBUTOR,
    UNSUPPORTED;

    public static String toString(Context context, Role role) {
        switch (role) {
            case SUPER_ADMIN:
                return context.getString(R.string.role_super_admin);
            case ADMIN:
                return context.getString(R.string.role_admin);
            case EDITOR:
                return context.getString(R.string.role_editor);
            case AUTHOR:
                return context.getString(R.string.role_author);
            case CONTRIBUTOR:
                return context.getString(R.string.role_contributor);
            case UNSUPPORTED:
                return context.getString(R.string.role_unsupported);
            default:
                return "";
        }
    }

    public static Role fromString(String value) {
        if (value == null)
            return Role.UNSUPPORTED;
        if (value.equals("administrator"))
            return Role.ADMIN;
        if (value.equals("editor"))
            return Role.EDITOR;
        if (value.equals("author"))
            return Role.AUTHOR;
        if (value.equals("contributor"))
            return Role.CONTRIBUTOR;
        return Role.UNSUPPORTED;
    }

    public static int backgroundColor(Context context, Role role) {
        switch (role) {
            case SUPER_ADMIN:
                return ContextCompat.getColor(context, R.color.orange_fire);
            case ADMIN:
                return ContextCompat.getColor(context, R.color.grey_dark);
            case EDITOR:
                return ContextCompat.getColor(context, R.color.blue_dark);
            case AUTHOR:
                return ContextCompat.getColor(context, R.color.blue_wordpress);
            case CONTRIBUTOR:
                return ContextCompat.getColor(context, R.color.blue_wordpress);
            case UNSUPPORTED:
                return ContextCompat.getColor(context, R.color.blue_wordpress);
            default:
                return ContextCompat.getColor(context, R.color.blue_wordpress);
        }
    }
}
