package org.wordpress.android.fluxc.annotations.endpoint;

public class WCWPAPIEndpoint {
    private static final String WC_PREFIX_V3 = "wc/v3";

    private static final String WC_PREFIX_V1 = "wc/v1";

    private static final String WC_PREFIX_V2 = "wc/v2";

    private static final String WC_PREFIX_V4 = "wc/v4";

    private static final String WC_PREFIX_V4_ANALYTICS = "wc-analytics";

    private final String mEndpoint;

    public WCWPAPIEndpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public WCWPAPIEndpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public WCWPAPIEndpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getPathV3() {
        return "/" + WC_PREFIX_V3 + mEndpoint;
    }

    public String getPathV2() {
        return "/" + WC_PREFIX_V2 + mEndpoint;
    }

    public String getPathV1() {
        return "/" + WC_PREFIX_V1 + mEndpoint;
    }

    public String getPathV4() {
        return "/" + WC_PREFIX_V4 + mEndpoint;
    }

    public String getPathV4Analytics() {
        return "/" + WC_PREFIX_V4_ANALYTICS + mEndpoint;
    }
}
