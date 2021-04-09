package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wordpress.android.fluxc.network.OkHttpStack;
import org.wordpress.android.fluxc.network.RetryOnRedirectBasicNetwork;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArray;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArrayDeserializer;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalseDeserializer;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
import okhttp3.OkHttpClient;

@Module
public class ReleaseNetworkModule {
    private static final String DEFAULT_CACHE_DIR = "volley-fluxc";
    private static final int NETWORK_THREAD_POOL_SIZE = 10;

    private RequestQueue newRetryOnRedirectRequestQueue(OkHttpClient.Builder okHttpClientBuilder, Context appContext) {
        Network network = new RetryOnRedirectBasicNetwork(new OkHttpStack(okHttpClientBuilder));
        return createRequestQueue(network, appContext);
    }

    private RequestQueue newRequestQueue(OkHttpClient.Builder okHttpClientBuilder, Context appContext) {
        Network network = new BasicNetwork(new OkHttpStack(okHttpClientBuilder));
        return createRequestQueue(network, appContext);
    }

    private RequestQueue createRequestQueue(Network network, Context appContext) {
        File cacheDir = new File(appContext.getCacheDir(), DEFAULT_CACHE_DIR);
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, NETWORK_THREAD_POOL_SIZE);
        queue.start();
        return queue;
    }

    @Singleton
    @Named("regular")
    @Provides
    public RequestQueue provideRequestQueue(@Named("regular") OkHttpClient.Builder okHttpClientBuilder,
                                            Context appContext) {
        return newRequestQueue(okHttpClientBuilder, appContext);
    }

    @Singleton
    @Named("no-redirects")
    @Provides
    public RequestQueue provideNoRedirectsRequestQueue(@Named("no-redirects") OkHttpClient.Builder okHttpClientBuilder,
                                                       Context appContext) {
        return newRetryOnRedirectRequestQueue(okHttpClientBuilder, appContext);
    }

    @Singleton
    @Named("custom-ssl")
    @Provides
    public RequestQueue provideRequestQueueCustomSSL(@Named("custom-ssl") OkHttpClient.Builder okHttpClientBuilder,
                                                     Context appContext) {
        return newRequestQueue(okHttpClientBuilder, appContext);
    }

    @Singleton
    @Provides
    public CoroutineContext provideCoroutineContext() {
        return Dispatchers.getDefault();
    }

    @Singleton
    @Provides
    public Gson provideGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setLenient();
        gsonBuilder.registerTypeHierarchyAdapter(JsonObjectOrFalse.class, new JsonObjectOrFalseDeserializer());
        gsonBuilder.registerTypeHierarchyAdapter(JsonObjectOrEmptyArray.class,
                new JsonObjectOrEmptyArrayDeserializer());
        return gsonBuilder.create();
    }
}
