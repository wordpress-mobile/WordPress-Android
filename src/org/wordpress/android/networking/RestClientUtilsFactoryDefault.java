package org.wordpress.android.networking;

import com.android.volley.RequestQueue;

public class RestClientUtilsFactoryDefault implements RestClientUtilsFactoryAbstract {
    public RestClientUtilsInterface make(RequestQueue queue, Authenticator authenticator) {
        return new RestClientUtils(queue, authenticator);
    }
}
