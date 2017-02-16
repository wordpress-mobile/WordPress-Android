package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.OkHttpStack;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class ReleaseNetworkModule {
    private static final String DEFAULT_CACHE_DIR = "volley-fluxc";
    private static final int NETWORK_THREAD_POOL_SIZE = 10;

    private RequestQueue newRequestQueue(OkHttpClient.Builder okHttpClientBuilder, Context appContext) {
        File cacheDir = new File(appContext.getCacheDir(), DEFAULT_CACHE_DIR);
        Network network = new BasicNetwork(new OkHttpStack(okHttpClientBuilder));
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
    @Named("custom-ssl")
    @Provides
    public RequestQueue provideRequestQueueCustomSSL(@Named("custom-ssl") OkHttpClient.Builder okHttpClientBuilder,
                                                     Context appContext) {
        return newRequestQueue(okHttpClientBuilder, appContext);
    }

    @Singleton
    @Provides
    public Authenticator provideAuthenticator(Dispatcher dispatcher, AppSecrets appSecrets,
                                              @Named("regular") RequestQueue requestQueue) {
        return new Authenticator(dispatcher, requestQueue, appSecrets);
    }

    @Singleton
    @Provides
    public UserAgent provideUserAgent(Context appContext) {
        return new UserAgent(appContext);
    }

    @Singleton
    @Provides
    public BaseXMLRPCClient provideBaseXMLRPCClient(Dispatcher dispatcher,
                                                    @Named("custom-ssl") RequestQueue requestQueue,
                                                    AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new BaseXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public BaseWPAPIRestClient provideBaseWPAPIClient(Dispatcher dispatcher,
                                                    @Named("custom-ssl") RequestQueue requestQueue,
                                                    UserAgent userAgent) {
        return new BaseWPAPIRestClient(dispatcher, requestQueue, userAgent);
    }

    @Singleton
    @Provides
    public SiteRestClient provideSiteRestClient(Context appContext, Dispatcher dispatcher,
                                                @Named("regular") RequestQueue requestQueue,
                                                AppSecrets appSecrets,
                                                AccessToken token, UserAgent userAgent) {
        return new SiteRestClient(appContext, dispatcher, requestQueue, appSecrets, token, userAgent);
    }

    @Singleton
    @Provides
    public SiteXMLRPCClient provideSiteXMLRPCClient(Dispatcher dispatcher,
                                                    @Named("custom-ssl") RequestQueue requestQueue,
                                                    AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new SiteXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public MediaRestClient provideMediaRestClient(Context appContext, Dispatcher dispatcher,
                                                  @Named("regular") RequestQueue requestQueue,
                                                  @Named("regular") OkHttpClient.Builder okHttpClientBuilder,
                                                  AccessToken token, UserAgent userAgent) {
        return new MediaRestClient(appContext, dispatcher, requestQueue, okHttpClientBuilder, token, userAgent);
    }

    @Singleton
    @Provides
    public MediaXMLRPCClient provideMediaXMLRPCClient(Dispatcher dispatcher,
                                                      @Named("custom-ssl") RequestQueue requestQueue,
                                                      @Named("custom-ssl") OkHttpClient.Builder okHttpClientBuilder,
                                                      AccessToken token, UserAgent userAgent,
                                                      HTTPAuthManager httpAuthManager) {
        return new MediaXMLRPCClient(dispatcher, requestQueue, okHttpClientBuilder, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public AccountRestClient provideAccountRestClient(Context appContext, Dispatcher dispatcher,
                                                      @Named("regular") RequestQueue requestQueue,
                                                      AppSecrets appSecrets,
                                                      AccessToken token, UserAgent userAgent) {
        return new AccountRestClient(appContext, dispatcher, requestQueue, appSecrets, token, userAgent);
    }

    @Singleton
    @Provides
    public PostRestClient providePostRestClient(Context appContext, Dispatcher dispatcher,
                                                @Named("regular") RequestQueue requestQueue,
                                                AccessToken token, UserAgent userAgent) {
        return new PostRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public PostXMLRPCClient providePostXMLRPCClient(Dispatcher dispatcher,
                                                    @Named("custom-ssl") RequestQueue requestQueue,
                                                    AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new PostXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public CommentRestClient provideCommentRestClient(Context appContext, Dispatcher dispatcher,
                                                      @Named("regular") RequestQueue requestQueue,
                                                      AccessToken token, UserAgent userAgent) {
        return new CommentRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public CommentXMLRPCClient provideCommentXMLRPCClient(Dispatcher dispatcher,
                                                       @Named("custom-ssl") RequestQueue requestQueue,
                                                       AccessToken token,
                                                       UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new CommentXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public TaxonomyRestClient provideTaxonomyRestClient(Context appContext, Dispatcher dispatcher,
                                                        @Named("regular") RequestQueue requestQueue,
                                                        AccessToken token, UserAgent userAgent) {
        return new TaxonomyRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public TaxonomyXMLRPCClient provideTaxonomyXMLRPCClient(Dispatcher dispatcher,
                                                            @Named("custom-ssl") RequestQueue requestQueue,
                                                            AccessToken token,
                                                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new TaxonomyXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public SelfHostedEndpointFinder provideSelfHostedEndpointFinder(Dispatcher dispatcher,
                                                                    BaseXMLRPCClient baseXMLRPCClient,
                                                                    BaseWPAPIRestClient baseWPAPIRestClient) {
        return new SelfHostedEndpointFinder(dispatcher, baseXMLRPCClient, baseWPAPIRestClient);
    }

    @Singleton
    @Provides
    public AccessToken provideAccountToken(Context appContext) {
        return new AccessToken(appContext);
    }

    @Singleton
    @Provides
    public HTTPAuthManager provideHTTPAuthManager() {
        return new HTTPAuthManager();
    }

    @Singleton
    @Provides
    public MemorizingTrustManager provideMemorizingTrustManager(Context appContext) {
        return new MemorizingTrustManager(appContext);
    }
}
