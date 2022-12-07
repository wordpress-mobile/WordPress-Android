package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WPAPIHeadRequest extends BaseRequest<String> {
    private static final Pattern LINK_PATTERN = Pattern.compile("^<(.*)>; rel=\"https://api.w.org/\"$");

    private final Listener<String> mListener;
    private String mResponseLinkHeader;

    public WPAPIHeadRequest(String url, Listener<String> listener, BaseErrorListener errorListener) {
        super(Method.HEAD, url, errorListener);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(extractEndpointFromLinkHeader(mResponseLinkHeader));
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        mResponseLinkHeader = response.headers.get("Link");
        return Response.success("", HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        // no op
        return new WPAPINetworkError(error, null);
    }

    private static String extractEndpointFromLinkHeader(String linkHeader) {
        if (linkHeader != null) {
            Matcher matcher = LINK_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
