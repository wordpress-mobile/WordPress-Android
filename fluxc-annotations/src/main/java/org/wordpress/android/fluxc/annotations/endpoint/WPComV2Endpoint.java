package org.wordpress.android.fluxc.annotations.endpoint;

public class WPComV2Endpoint {
    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com";
    private static final String WPCOM_V2_PREFIX = WPCOM_REST_PREFIX + "/wpcom/v2";

    private final String mEndpoint;

    public WPComV2Endpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public WPComV2Endpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public WPComV2Endpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getUrl() {
        return WPCOM_V2_PREFIX + mEndpoint;
    }
}
