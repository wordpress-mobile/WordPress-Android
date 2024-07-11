package org.wordpress.android.fluxc.network.discovery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WPAPIHeadRequest extends BaseRequest<String> {
    private static final Pattern LINK_PATTERN = Pattern.compile("^<(.*)>; rel=\"https://api.w.org/\"$");

    @NonNull private final Listener<String> mListener;
    @Nullable private String mResponseLinkHeader;

    public WPAPIHeadRequest(
            @NonNull String url,
            @NonNull Listener<String> listener,
            @NonNull BaseErrorListener errorListener) {
        super(Method.HEAD, url, errorListener);
        mListener = listener;
    }

    @Override
    protected void deliverResponse(@NonNull String response) {
        mListener.onResponse(extractEndpointFromLinkHeader(mResponseLinkHeader));
    }

    @NonNull
    @Override
    protected Response<String> parseNetworkResponse(@NonNull NetworkResponse response) {
        Map<String, String> headers = response.headers;
        if (headers != null) {
            mResponseLinkHeader = headers.get("Link");
            return Response.success("", HttpHeaderParser.parseCacheHeaders(response));
        } else {
            return Response.error(new ParseError(new Exception("No headers in response")));
        }
    }

    @NonNull
    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        // no op
        return new WPAPINetworkError(error, null);
    }

    @Nullable
    private static String extractEndpointFromLinkHeader(@Nullable String linkHeader) {
        if (linkHeader != null) {
            Matcher matcher = LINK_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
