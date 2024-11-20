package org.wordpress.android.fluxc.network.rest.wpcom.auth;

import androidx.annotation.NonNull;

public class AppSecrets {
    @NonNull private final String mAppId;
    @NonNull private final String mAppSecret;
    @NonNull private final String mRedirectUri;

    public AppSecrets(@NonNull String appId, @NonNull String appSecret, @NonNull String redirectUri) {
        mAppId = appId;
        mAppSecret = appSecret;
        mRedirectUri = redirectUri;
    }

    public @NonNull String getAppId() {
        return mAppId;
    }

    public @NonNull String getAppSecret() {
        return mAppSecret;
    }

    public @NonNull String getRedirectUri() {
        return mRedirectUri;
    }
}
