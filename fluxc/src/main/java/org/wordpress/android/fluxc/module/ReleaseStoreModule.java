package org.wordpress.android.fluxc.module;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.network.xmlrpc.comment.CommentXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.UploadStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseStoreModule {
    @Provides
    @Singleton
    public SiteStore provideSiteStore(Dispatcher dispatcher, SiteRestClient siteRestClient,
                                      SiteXMLRPCClient siteXMLRPCClient) {
        return new SiteStore(dispatcher, siteRestClient, siteXMLRPCClient);
    }

    @Provides
    @Singleton
    public MediaStore provideMediaStore(Dispatcher dispatcher, MediaRestClient mediaRestClient,
                                        MediaXMLRPCClient mediaXMLRPCClient, UploadStore uploadStore) {
        return new MediaStore(dispatcher, mediaRestClient, mediaXMLRPCClient, uploadStore);
    }

    @Provides
    @Singleton
    public AccountStore provideUserStore(Dispatcher dispatcher, AccountRestClient accountRestClient,
                                         SelfHostedEndpointFinder selfHostedEndpointFinder, Authenticator authenticator,
                                         AccessToken accessToken) {
        return new AccountStore(dispatcher, accountRestClient, selfHostedEndpointFinder, authenticator, accessToken);
    }

    @Provides
    @Singleton
    public PostStore providePostStore(Dispatcher dispatcher, PostRestClient postRestClient,
                                      PostXMLRPCClient postXMLRPCClient, UploadStore uploadStore) {
        return new PostStore(dispatcher, postRestClient, postXMLRPCClient, uploadStore);
    }

    @Provides
    @Singleton
    public CommentStore provideCommentStore(Dispatcher dispatcher, CommentRestClient restClient,
                                            CommentXMLRPCClient xmlrpcClient) {
        return new CommentStore(dispatcher, restClient, xmlrpcClient);
    }

    @Provides
    @Singleton
    public TaxonomyStore provideTaxonomyStore(Dispatcher dispatcher, TaxonomyRestClient taxonomyRestClient,
                                              TaxonomyXMLRPCClient taxonomyXMLRPCClient) {
        return new TaxonomyStore(dispatcher, taxonomyRestClient, taxonomyXMLRPCClient);
    }

    @Provides
    @Singleton
    public PluginStore providePluginStore(Dispatcher dispatcher, PluginRestClient pluginRestClient,
                                          PluginWPOrgClient pluginWPOrgClient) {
        return new PluginStore(dispatcher, pluginRestClient, pluginWPOrgClient);
    }

    @Provides
    @Singleton
    public UploadStore provideUploadStore(Dispatcher dispatcher) {
        return new UploadStore(dispatcher);
    }
}
