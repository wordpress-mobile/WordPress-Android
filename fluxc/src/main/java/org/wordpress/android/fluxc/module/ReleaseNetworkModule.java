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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;

@Module
public class ReleaseNetworkModule {
    private static final String DEFAULT_CACHE_DIR = "volley-fluxc";
    private static final int NETWORK_THREAD_POOL_SIZE = 10;

    private RequestQueue newRequestQueue(OkUrlFactory okUrlFactory, Context appContext) {
        File cacheDir = new File(appContext.getCacheDir(), DEFAULT_CACHE_DIR);
        Network network = new BasicNetwork(new OkHttpStack(okUrlFactory));
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, NETWORK_THREAD_POOL_SIZE);
        queue.start();
        return queue;
    }

    @Provides
    @Named("regular")
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder().build();
    }

    @Singleton
    @Named("regular")
    @Provides
    public OkUrlFactory provideOkUrlFactory(@Named("regular") OkHttpClient okHttpClient) {
        return new OkUrlFactory(okHttpClient);
    }

    @Singleton
    @Named("regular")
    @Provides
    public RequestQueue provideRequestQueue(@Named("regular") OkUrlFactory okUrlFactory, Context appContext) {
        return newRequestQueue(okUrlFactory, appContext);
    }

    @Provides
    @Named("custom-ssl")
    public OkHttpClient provideOkHttpClientCustomSSL(MemorizingTrustManager memorizingTrustManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{memorizingTrustManager}, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            AppLog.e(T.API, e);
        }
        return builder.build();
    }

    @Singleton
    @Named("custom-ssl")
    @Provides
    public OkUrlFactory provideOkUrlFactoryCustomSSL(@Named("custom-ssl") OkHttpClient okHttpClient) {
        return new OkUrlFactory(okHttpClient);
    }

    @Singleton
    @Named("custom-ssl")
    @Provides
    public RequestQueue provideRequestQueueCustomSSL(@Named("custom-ssl") OkUrlFactory okUrlFactory,
                                                     Context appContext) {
        return newRequestQueue(okUrlFactory, appContext);
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
                                                  @Named("regular") OkHttpClient okHttpClient,
                                                  AccessToken token, UserAgent userAgent) {
        return new MediaRestClient(appContext, dispatcher, requestQueue, okHttpClient, token, userAgent);
    }

    @Singleton
    @Provides
    public MediaXMLRPCClient provideMediaXMLRPCClient(Dispatcher dispatcher,
                                                      @Named("custom-ssl") RequestQueue requestQueue,
                                                      @Named("custom-ssl") OkHttpClient okClient,
                                                      AccessToken token, UserAgent userAgent,
                                                      HTTPAuthManager httpAuthManager) {
        return new MediaXMLRPCClient(dispatcher, requestQueue, okClient, token, userAgent, httpAuthManager);
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
                                                                    BaseXMLRPCClient baseXMLRPCClient) {
        return new SelfHostedEndpointFinder(dispatcher, baseXMLRPCClient);
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
