package org.wordpress.android.login;

import android.content.Intent;

public enum LoginMode {
    FULL,
    SELFHOSTED_ONLY,
    WPCOM_LOGIN_ONLY,
    JETPACK_STATS,
    WPCOM_LOGIN_DEEPLINK,
    WPCOM_REAUTHENTICATE,
    SHARE_INTENT,
    WOO_LOGIN_ONLY;

    private static final String ARG_LOGIN_MODE = "ARG_LOGIN_MODE";

    public static LoginMode fromIntent(Intent intent) {
        if (intent.hasExtra(ARG_LOGIN_MODE)) {
            return LoginMode.valueOf(intent.getStringExtra(ARG_LOGIN_MODE));
        } else {
            return FULL;
        }
    }

    public void putInto(Intent intent) {
        intent.putExtra(ARG_LOGIN_MODE, this.name());
    }
}
