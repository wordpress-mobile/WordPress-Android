package org.wordpress.android.networking;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class RestClientFactory {
    public static RestClientFactoryAbstract sFactory;

    public static RestClient instantiate(RequestQueue queue) {
        if (sFactory == null) {
            sFactory = new RestClientFactoryDefault();
        }
        AppLog.v(T.UTILS, "instantiate RestClient using sFactory: " + sFactory.getClass());
        return sFactory.make(queue);
    }
}
