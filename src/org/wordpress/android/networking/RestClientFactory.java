package org.wordpress.android.networking;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class RestClientFactory {
    public static RestClientFactoryAbstract factory;

    public static RestClient instantiate(RequestQueue queue) {
        if (factory == null) {
            factory = new RestClientFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate RestClient using factory: " + factory.getClass());
        return factory.make(queue);
    }
}
