package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.fluxc.network.BaseRequest;

import java.io.UnsupportedEncodingException;

public class DiscoveryRequest extends BaseRequest<String> {
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("text/xml; charset=%s", PROTOCOL_CHARSET);

    @NonNull private final Listener<String> mListener;

    public DiscoveryRequest(
            @NonNull String url,
            @NonNull Listener<String> listener,
            @NonNull BaseErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(@NonNull String response) {
        mListener.onResponse(response);
    }

    @NonNull
    @Override
    protected Response<String> parseNetworkResponse(@NonNull NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

    @NonNull
    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        // no op
        return error;
    }

    @NonNull
    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }
}
