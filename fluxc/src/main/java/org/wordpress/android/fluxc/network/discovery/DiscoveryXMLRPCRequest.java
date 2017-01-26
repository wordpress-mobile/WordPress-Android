package org.wordpress.android.fluxc.network.discovery;

import android.support.annotation.NonNull;

import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.action.AuthenticationAction;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;

/**
 * A custom XMLRPCRequest intended for XML-RPC discovery, which doesn't emit global
 * {@link AuthenticationAction#AUTHENTICATE_ERROR} events.
 */
public class DiscoveryXMLRPCRequest extends XMLRPCRequest {
    DiscoveryXMLRPCRequest(String url, XMLRPC method, Listener listener, BaseErrorListener errorListener) {
        super(url, method, null, listener, errorListener);
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        // no op
        return error;
    }
}
