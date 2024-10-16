package org.wordpress.android.fluxc.annotations.endpoint;

public class WPComV3Endpoint {
    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com";
    private static final String WPCOM_V3_PREFIX = WPCOM_REST_PREFIX + "/wpcom/v3";

    private final String mEndpoint;

    public WPComV3Endpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public WPComV3Endpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public WPComV3Endpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getUrl() {
        return WPCOM_V3_PREFIX + mEndpoint;
    }
}
