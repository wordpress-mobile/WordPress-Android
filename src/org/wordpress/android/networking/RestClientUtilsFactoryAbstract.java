package org.wordpress.android.networking;

import com.android.volley.RequestQueue;

public interface RestClientUtilsFactoryAbstract {
    public RestClientUtilsInterface make(RequestQueue queue, Authenticator authenticator);
}
