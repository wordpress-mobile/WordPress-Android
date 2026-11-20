package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.network.BaseRequestFuture;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient;
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers;
import org.wordpress.android.util.AppLog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.TIMEOUT_MS;

@SuppressWarnings("CommentedOutCode")
@Singleton
public class DiscoveryWPAPIRestClient extends BaseWPAPIRestClient {
    @Inject public DiscoveryWPAPIRestClient(
            Dispatcher dispatcher,
            @Named(OkHttpClientQualifiers.CUSTOM_SSL) RequestQueue requestQueue,
            UserAgent userAgent) {
        super(dispatcher, requestQueue, userAgent);
    }

    @Nullable
    public String discoverWPAPIBaseURL(@NonNull String url) {
        BaseRequestFuture<String> future = BaseRequestFuture.newFuture();
        WPAPIHeadRequest request = new WPAPIHeadRequest(url, future, future);
        add(request);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(AppLog.T.API, "Couldn't get HEAD response from server.");
        } catch (ExecutionException e) {
            // TODO: Add support for HTTP AUTH and self-signed SSL WP-API sites
//            if (e.getCause() instanceof AuthFailureError) {
//                throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
//            } else if (e.getCause() instanceof NoConnectionError && e.getCause().getCause() != null
//                    && e.getCause().getCause() instanceof SSLHandshakeException) {
//                // In the event of an SSL error we should stop attempting discovery
//                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
//            }
        }
        return null;
    }
}
