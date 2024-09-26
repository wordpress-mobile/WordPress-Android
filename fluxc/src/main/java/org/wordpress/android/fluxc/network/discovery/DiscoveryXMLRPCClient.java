package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.network.BaseRequestFuture;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryException;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLHandshakeException;

import static org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.TIMEOUT_MS;

@Singleton
public class DiscoveryXMLRPCClient extends BaseXMLRPCClient {
    @Inject public DiscoveryXMLRPCClient(
            Dispatcher dispatcher,
            @Named("custom-ssl") RequestQueue requestQueue,
            UserAgent userAgent,
            HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    /**
     * Obtain the HTML response from a GET request for the given URL.
     */
    @Nullable
    public String getResponse(@NonNull String url) throws DiscoveryException {
        BaseRequestFuture<String> future = BaseRequestFuture.newFuture();
        DiscoveryRequest request = new DiscoveryRequest(url, future, future);
        add(request);

        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(AppLog.T.API, "Couldn't get XML-RPC response");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AuthFailureError) {
                NetworkResponse networkResponse = ((AuthFailureError) e.getCause()).networkResponse;
                if (networkResponse == null) {
                    return null;
                }

                if (networkResponse.statusCode == 401) {
                    throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
                } else if (networkResponse.statusCode == 403) {
                    throw new DiscoveryException(DiscoveryError.XMLRPC_FORBIDDEN, url);
                }
            } else if (e.getCause() instanceof NoConnectionError
                       && e.getCause().getCause() instanceof SSLHandshakeException
                       && e.getCause().getCause().getCause() instanceof CertificateException) {
                // In the event of an SSL handshake error we should stop attempting discovery
                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
            }
        }
        return null;
    }

    /**
     * Perform a system.listMethods call on the given URL.
     */
    @Nullable
    public Object[] listMethods(@NonNull String url) throws DiscoveryException {
        if (!UrlUtils.isValidUrlAndHostNotNull(url)) {
            AppLog.e(AppLog.T.NUX, "Invalid URL: " + url);
            throw new DiscoveryException(DiscoveryError.INVALID_URL, url);
        }

        AppLog.i(AppLog.T.NUX, "Trying system.listMethods on the following URL: " + url);

        BaseRequestFuture<Object[]> future = BaseRequestFuture.newFuture();
        DiscoveryXMLRPCRequest request = new DiscoveryXMLRPCRequest(url, XMLRPC.LIST_METHODS, future, future);
        add(request);

        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            AppLog.e(AppLog.T.API, "Couldn't get XML-RPC response.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AuthFailureError) {
                NetworkResponse networkResponse = ((AuthFailureError) e.getCause()).networkResponse;
                if (networkResponse == null) {
                    return null;
                }

                if (networkResponse.statusCode == 401) {
                    throw new DiscoveryException(DiscoveryError.HTTP_AUTH_REQUIRED, url);
                } else if (networkResponse.statusCode == 403) {
                    throw new DiscoveryException(DiscoveryError.XMLRPC_FORBIDDEN, url);
                }
            } else if (e.getCause() instanceof NoConnectionError
                       && e.getCause().getCause() instanceof SSLHandshakeException
                       && e.getCause().getCause().getCause() instanceof CertificateException) {
                // In the event of an SSL handshake error we should stop attempting discovery
                throw new DiscoveryException(DiscoveryError.ERRONEOUS_SSL_CERTIFICATE, url);
            } else if (e.getCause() instanceof ServerError) {
                NetworkResponse networkResponse = ((ServerError) e.getCause()).networkResponse;
                if (networkResponse == null) {
                    return null;
                }

                if (networkResponse.statusCode == 405 && !new String(networkResponse.data).contains(
                        "XML-RPC server accepts POST requests only.")) {
                    // XML-RPC is blocked by the server (POST request returns a 405 "Method Not Allowed" error)
                    // We exclude the case where Volley followed a 301 redirect and tried to GET the xmlrpc endpoint,
                    // which also returns a 405 error but with the message "XML-RPC server accepts POST requests only."
                    throw new DiscoveryException(DiscoveryError.XMLRPC_BLOCKED, url);
                }
            }
        }
        return null;
    }
}
