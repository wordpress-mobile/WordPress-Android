package org.wordpress.android.fluxc.network.rest.wpcom;

@SuppressWarnings("unused")
public class WPComEndpoint {
    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com/rest";
    private static final String WPCOM_PREFIX_V1 = WPCOM_REST_PREFIX + "/v1";
    private static final String WPCOM_PREFIX_V1_1 = WPCOM_REST_PREFIX + "/v1.1";
    private static final String WPCOM_PREFIX_V1_2 = WPCOM_REST_PREFIX + "/v1.2";
    private static final String WPCOM_PREFIX_V1_3 = WPCOM_REST_PREFIX + "/v1.3";

    private final String mEndpoint;

    protected WPComEndpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    protected WPComEndpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public String getEndpoint() {
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
