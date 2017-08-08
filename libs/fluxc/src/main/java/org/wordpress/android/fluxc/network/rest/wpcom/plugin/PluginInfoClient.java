package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginInfoClient extends BaseWPAPIRestClient {
    @Inject
    public PluginInfoClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent) {
        super(dispatcher, requestQueue, userAgent);
    }
}
