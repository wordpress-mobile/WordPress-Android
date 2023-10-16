package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;

import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.action.AuthenticationAction;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;

/**
 * A custom XMLRPCRequest intended for XML-RPC discovery, which doesn't emit global
 * {@link AuthenticationAction#AUTHENTICATE_ERROR} events.
 */
public class DiscoveryXMLRPCRequest extends XMLRPCRequest {
    DiscoveryXMLRPCRequest(@NonNull String url, XMLRPC method, Listener<? super Object[]> listener,
                           BaseErrorListener errorListener) {
        super(url, method, null, listener, errorListener);
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        // no op
        return error;
    }
}
