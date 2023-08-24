package org.wordpress.android.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

public class RestClientFactoryDefault implements RestClientFactoryAbstract {
    @NonNull
    @Override
    public RestClient make(
            @Nullable RequestQueue queue
    ) {
        return new RestClient(queue);
    }

    @NonNull
    @Override
    public RestClient make(
            @Nullable RequestQueue queue,
            @NonNull RestClient.REST_CLIENT_VERSIONS version
    ) {
        return new RestClient(queue, version);
    }
}
