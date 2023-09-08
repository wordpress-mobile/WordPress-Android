package org.wordpress.android.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.wordpress.rest.RestClient;

public interface RestClientFactoryAbstract {
    @NonNull
    RestClient make(@Nullable RequestQueue queue);
    @NonNull
    RestClient make(@Nullable RequestQueue queue, @NonNull RestClient.REST_CLIENT_VERSIONS version);
}
