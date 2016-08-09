package org.wordpress.android.fluxc.network.rest.wpcom;

public enum WPCOMREST {
    // Me
    ME("/me/"),
    ME_SETTINGS("/me/settings/"),
    ME_SITES("/me/sites/"),

    // Sites
    SITES("/sites/"),
    SITES_NEW("/sites/new"),

    // Users
    USERS_NEW("/users/new"),

    // Media
    MEDIA_ALL("/sites/%s/media"),
    MEDIA_ITEM("/sites/%s/media/%s"),
    MEDIA_NEW("/sites/%s/media/new"),
    MEDIA_DELETE("/sites/%s/media/%s/delete");

    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com/rest";
    private static final String WPCOM_PREFIX_V1 = WPCOM_REST_PREFIX + "/v1";
    private static final String WPCOM_PREFIX_V1_1 = WPCOM_REST_PREFIX + "/v1.1";
    private static final String WPCOM_PREFIX_V1_2 = WPCOM_REST_PREFIX + "/v1.2";
    private static final String WPCOM_PREFIX_V1_3 = WPCOM_REST_PREFIX + "/v1.3";

    private final String mEndpoint;

    WPCOMREST(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }

    public String getUrlV1(String...args) {
        return WPCOM_PREFIX_V1 + String.format(mEndpoint, args);
    }

    public String getUrlV1_1(String...args) {
        return WPCOM_PREFIX_V1_1 + String.format(mEndpoint, args);
    }

    public String getUrlV1_2(String...args) {
        return WPCOM_PREFIX_V1_2 + String.format(mEndpoint, args);
    }

    public String getUrlV1_3(String...args) {
        return WPCOM_PREFIX_V1_3 + String.format(mEndpoint, args);
    }
}
