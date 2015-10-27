package org.wordpress.android.stores.module;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.stores.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.SiteStore;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseStoreModule {
    @Provides
    public SiteStore provideSiteStore(Dispatcher dispatcher, SiteRestClient siteRestClient,
                                      SiteXMLRPCClient siteXMLRPCClient) {
        return new SiteStore(dispatcher, siteRestClient, siteXMLRPCClient);
    }

    @Provides
    public AccountStore provideUserStore(Dispatcher dispatcher, AccountRestClient accountRestClient,
                                         Authenticator authenticator, AccessToken accessToken) {
        return new AccountStore(dispatcher, accountRestClient, authenticator, accessToken);
    }
}
