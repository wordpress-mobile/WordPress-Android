package org.wordpress.android.fluxc.network;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.HttpResponse;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Modified version of https://gist.github.com/LOG-TAG/3ad1c191b3ca7eab3ea6834386e30eb9
 * <p>
 * OkHttp backed {@link BaseHttpStack BaseHttpStack} that does not
 * use okhttp-urlconnection
 */
public class OkHttpStack extends BaseHttpStack {
    // OkHttp treats a backslash like "/" and drops tabs and newlines inside path segments
    private static final Pattern DOT_SEGMENT = Pattern.compile("(?:^|[/\\\\])(?:\\.|%2e){1,2}(?:[/\\\\]|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IGNORED_PATH_CHARS = Pattern.compile("[\\t\\n\\f\\r]");
    // group 1 is the path, without the scheme, authority, query or fragment
    private static final Pattern URL_PATH = Pattern.compile("^(?:[a-z][a-z0-9+.-]*://)?[^/\\\\?#]*([^?#]*)",
            Pattern.CASE_INSENSITIVE);

    private final OkHttpClient mOkHttpClient;

    public OkHttpStack(final OkHttpClient okHttpClient) {
        this.mOkHttpClient = okHttpClient;
    }

    private static void setConnectionParametersForRequest(okhttp3.Request.Builder builder, Request<?> request)
            throws AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                // Ensure backwards compatibility.  Volley assumes a request with a null body is a GET.
                byte[] postBody = request.getBody();
                if (postBody != null) {
                    builder.post(RequestBody.create(MediaType.parse(request.getBodyContentType()), postBody));
                }
                break;
            case Request.Method.GET:
                builder.get();
                break;
            case Request.Method.DELETE:
                builder.delete(createRequestBody(request));
                break;
            case Request.Method.POST:
                builder.post(createRequestBody(request));
                break;
            case Request.Method.PUT:
                builder.put(createRequestBody(request));
                break;
            case Request.Method.HEAD:
                builder.head();
                break;
            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;
            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;
            case Request.Method.PATCH:
                builder.patch(createRequestBody(request));
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    @NonNull
    private static RequestBody createRequestBody(Request r) throws AuthFailureError {
        final byte[] body = r.getBody();
        if (body == null) {
            return RequestBody.create(null, new byte[]{});
        }
        return RequestBody.create(MediaType.parse(r.getBodyContentType()), body);
    }

    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        int timeoutMs = request.getTimeoutMs();

        final OkHttpClient timeoutAwareClient = mOkHttpClient.newBuilder()
                                                             .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                                                             .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                                                             .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                                                             .build();

        // OkHttp resolves "." and ".." path segments, even percent-encoded ones, so a request whose path
        // contains one would otherwise be sent to a different endpoint than the one it was built for. This isn't
        // an IOException because Volley would report that as a NoConnectionError
        if (hasDotSegment(request.getUrl())) {
            AppLog.e(T.API, "Rejected request whose URL path contains a dot segment");
            throw new IllegalArgumentException("URL contains a dot path segment");
        }

        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();
        okHttpRequestBuilder.url(request.getUrl());

        Map<String, String> headers = request.getHeaders();
        for (final String name : headers.keySet()) {
            String value = headers.get(name);
            if (value != null) {
                okHttpRequestBuilder.addHeader(name, value);
            }
        }
        for (final String name : additionalHeaders.keySet()) {
            String value = additionalHeaders.get(name);
            if (value != null) {
                okHttpRequestBuilder.addHeader(name, value);
            }
        }

        setConnectionParametersForRequest(okHttpRequestBuilder, request);


        okhttp3.Request okHttpRequest = okHttpRequestBuilder.build();
        Call okHttpCall = timeoutAwareClient.newCall(okHttpRequest);
        okhttp3.Response okHttpResponse = okHttpCall.execute();


        int code = okHttpResponse.code();
        ResponseBody body = okHttpResponse.body();
        InputStream content = body == null ? null : body.byteStream();
        int contentLength = body == null ? 0 : (int) body.contentLength();
        List<Header> responseHeaders = mapHeaders(okHttpResponse.headers());
        return new HttpResponse(code, responseHeaders, contentLength, content);
    }

    static boolean hasDotSegment(@NonNull String url) {
        // OkHttp trims leading and trailing whitespace before parsing
        Matcher matcher = URL_PATH.matcher(url.trim());
        return matcher.lookingAt()
               && DOT_SEGMENT.matcher(IGNORED_PATH_CHARS.matcher(matcher.group(1)).replaceAll("")).find();
    }

    private List<Header> mapHeaders(Headers responseHeaders) {
        List<Header> headers = new ArrayList<>();
        for (int i = 0, len = responseHeaders.size(); i < len; i++) {
            final String name = responseHeaders.name(i), value = responseHeaders.value(i);
            headers.add(new Header(name, value));
        }
        return headers;
    }
}
