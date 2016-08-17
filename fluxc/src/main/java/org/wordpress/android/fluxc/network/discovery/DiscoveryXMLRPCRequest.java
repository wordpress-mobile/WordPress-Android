package org.wordpress.android.fluxc.network.discovery;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.action.AuthenticationAction;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;

import java.util.List;

/**
 * A custom XMLRPCRequest intended for XML-RPC discovery, which doesn't emit global
 * {@link AuthenticationAction#AUTHENTICATE_ERROR} events.
 */
public class DiscoveryXMLRPCRequest extends XMLRPCRequest {
    private final ErrorListener mErrorListener;

    public DiscoveryXMLRPCRequest(String url, XMLRPC method, List<Object> params, Listener listener,
                                  BaseErrorListener errorListener) {
        super(url, method, params, listener, errorListener);
        mErrorListener = errorListener;
    }

    @Override
    public void deliverError(VolleyError error) {
        if (mErrorListener != null) {
            mErrorListener.onErrorResponse(error);
        }
    }
}
