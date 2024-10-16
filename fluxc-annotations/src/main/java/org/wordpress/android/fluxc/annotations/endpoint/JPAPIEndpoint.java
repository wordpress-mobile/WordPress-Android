package org.wordpress.android.fluxc.annotations.endpoint;

public class JPAPIEndpoint {
    private static final String JP_PREFIX_V4 = "jetpack/v4";

    private final String mEndpoint;

    public JPAPIEndpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public JPAPIEndpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public JPAPIEndpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getPathV4() {
        return "/" + JP_PREFIX_V4 + mEndpoint;
    }
}
