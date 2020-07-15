package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.OkHttpStack;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient;
import org.wordpress.android.fluxc.network.discovery.DiscoveryXMLRPCClient;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArray;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArrayDeserializer;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse;
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalseDeserializer;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.activity.ActivityLogRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.reader.ReaderRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.CommentsRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils;
import org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.vertical.VerticalRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
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
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Dispatchers;
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
    public Authenticator provideAuthenticator(Context appContext, Dispatcher dispatcher, AppSecrets appSecrets,
                                              @Named("regular") RequestQueue requestQueue) {
        return new Authenticator(appContext, dispatcher, requestQueue, appSecrets);
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
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new SiteXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
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
                                                      @Named("custom-ssl") OkHttpClient okHttpClient,
                                                      UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new MediaXMLRPCClient(dispatcher, requestQueue, okHttpClient, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public StockMediaRestClient provideStockMediaRestClient(Context appContext, Dispatcher dispatcher,
                                                            @Named("regular") RequestQueue requestQueue,
                                                            AccessToken token, UserAgent userAgent) {
        return new StockMediaRestClient(appContext, dispatcher, requestQueue, token, userAgent);
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
    public NotificationRestClient provideNotificationRestClient(Context appContext, Dispatcher dispatcher,
                                                        @Named("regular") RequestQueue requestQueue,
                                                        AccessToken token, UserAgent userAgent) {
        return new NotificationRestClient(appContext, dispatcher, requestQueue, token, userAgent);
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
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new PostXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
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
    public ActivityLogRestClient provideActivityLogRestClient(Context appContext, Dispatcher dispatcher,
                                                              @Named("regular") RequestQueue requestQueue,
                                                              AccessToken token, UserAgent userAgent,
                                                              WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new ActivityLogRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public AllTimeInsightsRestClient provideAllTimeInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                                      @Named("regular") RequestQueue requestQueue,
                                                                      AccessToken token, UserAgent userAgent,
                                                                      WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                                      StatsUtils statsUtils) {
        return new AllTimeInsightsRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public CommentsRestClient provideCommentsInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                                @Named("regular") RequestQueue requestQueue,
                                                                AccessToken token, UserAgent userAgent,
                                                                WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                                StatsUtils statsUtils) {
        return new CommentsRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public FollowersRestClient provideFollowersInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                                  @Named("regular") RequestQueue requestQueue,
                                                                  AccessToken token, UserAgent userAgent,
                                                                  WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                                  StatsUtils statsUtils) {
        return new FollowersRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public LatestPostInsightsRestClient provideLatestPostsInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                                     @Named("regular") RequestQueue requestQueue,
                                                                     AccessToken token, UserAgent userAgent,
                                                                     WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                                     StatsUtils statsUtils) {
        return new LatestPostInsightsRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public MostPopularRestClient provideMostPopularInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                                      @Named("regular") RequestQueue requestQueue,
                                                                      AccessToken token, UserAgent userAgent,
                                                                      WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new MostPopularRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public PublicizeRestClient providePublicizeInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                                  @Named("regular") RequestQueue requestQueue,
                                                                  AccessToken token, UserAgent userAgent,
                                                                  WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                                  StatsUtils statsUtils) {
        return new PublicizeRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public TagsRestClient provideTagsInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                        @Named("regular") RequestQueue requestQueue,
                                                        AccessToken token, UserAgent userAgent,
                                                        WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                        StatsUtils statsUtils) {
        return new TagsRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public TodayInsightsRestClient provideInsightsRestClient(Context appContext, Dispatcher dispatcher,
                                                             @Named("regular") RequestQueue requestQueue,
                                                             AccessToken token, UserAgent userAgent,
                                                             WPComGsonRequestBuilder wpComGsonRequestBuilder,
                                                             StatsUtils statsUtils) {
        return new TodayInsightsRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent, statsUtils);
    }

    @Singleton
    @Provides
    public JetpackRestClient provideJetpackRestClient(Context appContext, Dispatcher dispatcher,
                                                      @Named("regular") RequestQueue requestQueue,
                                                      AccessToken token, UserAgent userAgent,
                                                      WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new JetpackRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public CoroutineContext provideCoroutineContext() {
        return Dispatchers.getDefault();
    }

    @Singleton
    @Provides
    public CommentXMLRPCClient provideCommentXMLRPCClient(Dispatcher dispatcher,
                                                          @Named("custom-ssl") RequestQueue requestQueue,
                                                          UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new CommentXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
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
                                                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new TaxonomyXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public DiscoveryXMLRPCClient provideDiscoveryXMLRPCClient(Dispatcher dispatcher,
                                                              @Named("custom-ssl") RequestQueue requestQueue,
                                                              UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new DiscoveryXMLRPCClient(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public DiscoveryWPAPIRestClient provideDiscoveryWPAPIRestClient(Dispatcher dispatcher,
                                                                    @Named("custom-ssl") RequestQueue requestQueue,
                                                                    UserAgent userAgent) {
        return new DiscoveryWPAPIRestClient(dispatcher, requestQueue, userAgent);
    }

    @Singleton
    @Provides
    public ThemeRestClient provideThemeRestClient(Context appContext, Dispatcher dispatcher,
                                                  @Named("regular") RequestQueue requestQueue,
                                                  AccessToken token, UserAgent userAgent) {
        return new ThemeRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public PluginRestClient providePluginRestClient(Context appContext, Dispatcher dispatcher,
                                                    @Named("regular") RequestQueue requestQueue,
                                                    AccessToken token, UserAgent userAgent) {
        return new PluginRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public PluginWPOrgClient providePluginWPOrgClient(Dispatcher dispatcher,
                                                      @Named("regular") RequestQueue requestQueue,
                                                      UserAgent userAgent) {
        return new PluginWPOrgClient(dispatcher, requestQueue, userAgent);
    }

    @Singleton
    @Provides
    public SelfHostedEndpointFinder provideSelfHostedEndpointFinder(Dispatcher dispatcher,
                                                                    DiscoveryXMLRPCClient discoveryXMLRPCClient,
                                                                    DiscoveryWPAPIRestClient discoveryWPAPIRestClient) {
        return new SelfHostedEndpointFinder(dispatcher, discoveryXMLRPCClient, discoveryWPAPIRestClient);
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

    @Singleton
    @Provides
    public ReaderRestClient provideReaderRestClient(Context appContext, Dispatcher dispatcher,
                                                     @Named("regular") RequestQueue requestQueue,
                                                     AccessToken token, UserAgent userAgent) {
        return new ReaderRestClient(appContext, dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public VerticalRestClient provideVerticalRestClient(Context appContext, Dispatcher dispatcher,
                                                        @Named("regular") RequestQueue requestQueue,
                                                        AccessToken token, UserAgent userAgent,
                                                        WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new VerticalRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public PlanOffersRestClient providePlansRestClient(Context appContext, Dispatcher dispatcher,
                                                       @Named("regular") RequestQueue requestQueue,
                                                       AccessToken token, UserAgent userAgent,
                                                       WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new PlanOffersRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public WhatsNewRestClient provideWhatsNewRestClient(Context appContext, Dispatcher dispatcher,
                                                     @Named("regular") RequestQueue requestQueue,
                                                     AccessToken token, UserAgent userAgent,
                                                     WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new WhatsNewRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public TransactionsRestClient provideTransactionsRestClient(Context appContext, Dispatcher dispatcher,
                                                         @Named("regular") RequestQueue requestQueue,
                                                         AccessToken token, UserAgent userAgent,
                                                         WPComGsonRequestBuilder wpComGsonRequestBuilder) {
        return new TransactionsRestClient(dispatcher, wpComGsonRequestBuilder, appContext, requestQueue, token,
                userAgent);
    }

    @Singleton
    @Provides
    public EncryptedLogRestClient provideEncryptedLogRestClient(@Named("regular") RequestQueue requestQueue,
                                                                AppSecrets appSecrets) {
        return new EncryptedLogRestClient(requestQueue, appSecrets);
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
