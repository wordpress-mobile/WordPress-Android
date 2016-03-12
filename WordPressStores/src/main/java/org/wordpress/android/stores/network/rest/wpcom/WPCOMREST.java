package org.wordpress.android.stores.network.rest.wpcom;

import static org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient.WPCOM_PREFIX_V1;
import static org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient.WPCOM_PREFIX_V1_1;
import static org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient.WPCOM_PREFIX_V1_2;
import static org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient.WPCOM_PREFIX_V1_3;

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
}
