package org.wordpress.android.fluxc.annotations.endpoint;

public class WPAPIEndpoint {
    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com";
    private static final String WPAPI_PREFIX_V2 = "wp/v2";
    private static final String WPCOM_WPAPI_PREFIX = WPCOM_REST_PREFIX + "/wp/v2/sites/";

    private final String mEndpoint;

    public WPAPIEndpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public WPAPIEndpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public WPAPIEndpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getUrlV2() {
        return WPAPI_PREFIX_V2 + mEndpoint;
    }

    public String getWPComUrl(long siteId) {
        return WPCOM_WPAPI_PREFIX + siteId + mEndpoint;
    }
}
