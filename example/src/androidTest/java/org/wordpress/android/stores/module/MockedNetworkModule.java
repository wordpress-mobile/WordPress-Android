package org.wordpress.android.stores.module;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.OkHttpStack;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.ErrorListener;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.Listener;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator.Token;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.stores.network.xmlrpc.site.SiteXMLRPCClient;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.eq;

@Module
public class MockedNetworkModule {
    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient();
    }

    @Singleton
    @Provides
    public OkUrlFactory provideOkUrlFactory(OkHttpClient okHttpClient) {
        return new OkUrlFactory(okHttpClient);
    }

    @Singleton
    @Provides
    public RequestQueue provideRequestQueue(OkUrlFactory okUrlFactory, Context appContext) {
        return Volley.newRequestQueue(appContext, new OkHttpStack(okUrlFactory));
    }

    @Singleton
    @Provides
    public Authenticator provideAuthenticator(AppSecrets appSecrets, RequestQueue requestQueue) {
        Authenticator authenticator = new Authenticator(requestQueue, appSecrets);
        Authenticator spy = spy(authenticator);

        // Mock Authenticator with correct user: test/test
        doAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        Listener listener = (Listener) args[4];
                        listener.onResponse(new Token("deadparrot", "", "", "", ""));
                        return null;
                    }
                }
        ).when(spy).authenticate(eq("test"), eq("test"), anyString(), anyBoolean(),
                (Listener) any(), (ErrorListener) any());

        // Mock Authenticator with erroneous user: error/error
        doAnswer(
                new Answer() {
                    public Object answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        ErrorListener listener = (ErrorListener) args[5];
                        listener.onErrorResponse(null);
                        return null;
                    }
                }
        ).when(spy).authenticate(eq("error"), eq("error"), anyString(), anyBoolean(),
                (Listener) any(), (ErrorListener) any());
        return spy;
    }

    @Singleton
    @Provides
    public UserAgent provideUserAgent(Context appContext) {
        return new UserAgent(appContext);
    }

    @Singleton
    @Provides
    public SiteRestClient provideSiteRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                UserAgent userAgent) {
        return new SiteRestClient(dispatcher, requestQueue, token, userAgent);
    }

    @Singleton
    @Provides
    public SiteXMLRPCClient provideSiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken token,
                                                    UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        return new SiteXMLRPCClient(dispatcher, requestQueue, token, userAgent, httpAuthManager);
    }

    @Singleton
    @Provides
    public AccountRestClient provideAccountRestClient(Dispatcher dispatcher, RequestQueue requestQueue,
                                                      AccessToken token, UserAgent userAgent) {
        return new AccountRestClient(dispatcher, requestQueue, token, userAgent);
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
}
