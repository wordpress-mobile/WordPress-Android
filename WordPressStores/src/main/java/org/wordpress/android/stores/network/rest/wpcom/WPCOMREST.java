package org.wordpress.android.stores.network.rest.wpcom;

public enum WPCOMREST {
    ME("/me/"),
    ME_SITES("/me/sites/"),
    SITES("/sites/");

    private final String mEndpoint;

    WPCOMREST(String endpoint) {
        mEndpoint = endpoint;
    }

    @Override
    public String toString() {
        return mEndpoint;
    }
}
