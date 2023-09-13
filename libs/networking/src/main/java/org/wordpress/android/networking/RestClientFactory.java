package org.wordpress.android.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

public class RestClientFactory {
    private static RestClientFactoryAbstract sFactory;

    @NonNull
    public static RestClient instantiate(
            @Nullable RequestQueue queue
    ) {
        return instantiate(queue, RestClient.REST_CLIENT_VERSIONS.V1);
    }

    @NonNull
    public static RestClient instantiate(
            @Nullable RequestQueue queue,
            @NonNull RestClient.REST_CLIENT_VERSIONS version
    ) {
        if (sFactory == null) {
            sFactory = new RestClientFactoryDefault();
        }
        return sFactory.make(queue, version);
    }
}
