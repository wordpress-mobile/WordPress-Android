package org.wordpress.android.fluxc.network.rest.wpcom.reader;

import android.content.Context;

import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;

import javax.inject.Singleton;

@Singleton
public class ReaderRestClient extends BaseWPComRestClient {
    public ReaderRestClient(Context appContext, Dispatcher dispatcher,
                            RequestQueue requestQueue,
                            AccessToken accessToken,
                            UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }
}
