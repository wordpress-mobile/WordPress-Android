package org.wordpress.android.networking;

import com.android.volley.RequestQueue;

public class RestClientUtilsFactory {
    public static RestClientUtilsFactoryAbstract factory;

    public static RestClientUtilsInterface instantiate(RequestQueue queue, Authenticator authenticator) {
        if (factory == null) {
            factory = new RestClientUtilsFactoryDefault();
        }
        return factory.make(queue, authenticator);
    }
}
