package org.wordpress.android.login;

import android.content.Intent;

import androidx.annotation.NonNull;

public enum LoginMode {
    FULL,
    SELFHOSTED_ONLY,
    WPCOM_LOGIN_ONLY,
    JETPACK_LOGIN_ONLY,
    JETPACK_STATS,
    JETPACK_REST_CONNECT,

    // The user has the Jetpack app installed, is not logged in, but tries to use it
    // to open a link like https://wordpress.com/stats.
    WPCOM_LOGIN_DEEPLINK,
    WPCOM_REAUTHENTICATE,

    // The user has tried to share content from another app (like Google Photos) using the app, but they
    // haven't logged in yet.
    SHARE_INTENT;

    private static final String ARG_LOGIN_MODE = "ARG_LOGIN_MODE";

    @NonNull public static LoginMode fromIntent(@NonNull Intent intent) {
        if (intent.hasExtra(ARG_LOGIN_MODE)) {
            return LoginMode.valueOf(intent.getStringExtra(ARG_LOGIN_MODE));
        } else {
            return FULL;
        }
    }

    public void putInto(@NonNull Intent intent) {
        intent.putExtra(ARG_LOGIN_MODE, this.name());
    }
}
