package org.wordpress.android.fluxc.module;

import android.content.Context;

import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.scan.threat.ThreatMapper;
import org.wordpress.android.fluxc.network.RetryOnRedirectBasicNetwork;
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
import org.wordpress.android.fluxc.network.rest.wpcom.common.LikesResponseUtilsProvider;
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.experiments.ExperimentRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaResponseUtils;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.reader.ReaderRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.scan.ScanRestClient;
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
import org.wordpress.android.fluxc.utils.TimeZoneProvider;

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
