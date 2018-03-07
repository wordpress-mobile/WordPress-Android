package org.wordpress.android.networking;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

public interface RestClientFactoryAbstract {
    RestClient make(RequestQueue queue);
    RestClient make(RequestQueue queue, RestClient.REST_CLIENT_VERSIONS version);
}
