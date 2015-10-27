package org.wordpress.android.stores.network.rest.wpcom.auth;

public class AppSecrets {
    private final String mAppId;
    private final String mAppSecret;

    public AppSecrets(String appId, String appSecret) {
        mAppId = appId;
        mAppSecret = appSecret;
    }

    public String getAppId() {
        return mAppId;
    }

    public String getAppSecret() {
        return mAppSecret;
    }
}
