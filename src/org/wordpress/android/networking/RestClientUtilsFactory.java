package org.wordpress.android.networking;

import com.android.volley.RequestQueue;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class RestClientUtilsFactory {
    public static RestClientUtilsFactoryAbstract factory;

    public static RestClientUtilsInterface instantiate(RequestQueue queue, Authenticator authenticator) {
        if (factory == null) {
            factory = new RestClientUtilsFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate RestClientUtilsInterface using factory: " + factory.getClass());
        return factory.make(queue, authenticator);
    }
}
