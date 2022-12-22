package org.wordpress.android.fluxc.annotations.endpoint;

public class WPComEndpoint {
    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com";
    private static final String WPCOM_PREFIX_V1 = WPCOM_REST_PREFIX + "/rest/v1";
    private static final String WPCOM_PREFIX_V1_1 = WPCOM_REST_PREFIX + "/rest/v1.1";
    private static final String WPCOM_PREFIX_V1_2 = WPCOM_REST_PREFIX + "/rest/v1.2";
    private static final String WPCOM_PREFIX_V1_3 = WPCOM_REST_PREFIX + "/rest/v1.3";
    private static final String WPCOM_PREFIX_V1_5 = WPCOM_REST_PREFIX + "/rest/v1.5";
    private static final String WPCOM_PREFIX_V0 = WPCOM_REST_PREFIX;

    private final String mEndpoint;

    public WPComEndpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public WPComEndpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public WPComEndpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getUrlV1() {
        return WPCOM_PREFIX_V1 + mEndpoint;
    }

    public String getUrlV1_1() {
        return WPCOM_PREFIX_V1_1 + mEndpoint;
    }

    public String getUrlV1_2() {
        return WPCOM_PREFIX_V1_2 + mEndpoint;
    }

    public String getUrlV1_3() {
        return WPCOM_PREFIX_V1_3 + mEndpoint;
    }

    public String getUrlV1_5() {
        return WPCOM_PREFIX_V1_5 + mEndpoint;
    }

    public String getUrlV0() {
        return WPCOM_PREFIX_V0 + mEndpoint;
    }
}
