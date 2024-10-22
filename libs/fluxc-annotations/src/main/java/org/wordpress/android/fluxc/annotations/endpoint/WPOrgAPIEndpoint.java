package org.wordpress.android.fluxc.annotations.endpoint;

public class WPOrgAPIEndpoint {
    private static final String WPORG_API_PREFIX = "https://api.wordpress.org";

    private final String mEndpoint;

    public WPOrgAPIEndpoint(String endpoint) {
        mEndpoint = endpoint;
    }

    public WPOrgAPIEndpoint(String endpoint, long id) {
        this(endpoint + id + "/");
    }

    public WPOrgAPIEndpoint(String endpoint, String value) {
        this(endpoint + value + "/");
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public String getUrl() {
        if (mEndpoint.contains("plugins/info/1.0")) {
            // For the plugins-info endpoint for 1.0 specifically, we want to request JSON data
            // All other WP.org endpoints either return JSON by default, or their newest endpoint version does
            return WPORG_API_PREFIX + mEndpoint.substring(0, mEndpoint.length() - 1) + ".json";
        }
        return WPORG_API_PREFIX + mEndpoint;
    }
}
