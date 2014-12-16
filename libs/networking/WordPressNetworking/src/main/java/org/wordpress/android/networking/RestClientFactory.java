package org.wordpress.android.networking;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

public class RestClientFactory {
    private static RestClientFactoryAbstract sFactory;

    public static RestClient instantiate(RequestQueue queue) {
        return instantiate(queue, RestClient.REST_CLIENT_VERSIONS.V1);
    }

    public static RestClient instantiate(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version) {
        if (sFactory == null) {
            sFactory = new RestClientFactoryDefault();
        }
        return sFactory.make(queue, version);
    }
}
