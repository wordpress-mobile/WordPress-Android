package org.wordpress.android.fluxc.annotations.endpoint;

public class WCWPAPIEndpoint {
    private static final String WC_PREFIX_V2 = "wc/v2";

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

    public String getPathV2() {
        return "/" + WC_PREFIX_V2 + mEndpoint;
    }
}
