package org.wordpress.android.fluxc.network;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Interceptor;
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
    private final OkHttpClient.Builder mClientBuilder;

    public OkHttpStack(OkHttpClient.Builder clientBuilder) {
        this.mClientBuilder = clientBuilder;
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

    private static RequestBody createRequestBody(Request r) throws AuthFailureError {
        final byte[] body = r.getBody();
        if (body == null) {
            return null;
        }
        return RequestBody.create(MediaType.parse(r.getBodyContentType()), body);
    }

    @Override
    public HttpResponse executeRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        int timeoutMs = request.getTimeoutMs();

        mClientBuilder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        mClientBuilder.readTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        mClientBuilder.writeTimeout(timeoutMs, TimeUnit.MILLISECONDS);

        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();
        okHttpRequestBuilder.url(request.getUrl());

        Map<String, String> headers = request.getHeaders();
        for (final String name : headers.keySet()) {
            okHttpRequestBuilder.addHeader(name, headers.get(name));
        }
        for (final String name : additionalHeaders.keySet()) {
            okHttpRequestBuilder.addHeader(name, additionalHeaders.get(name));
        }

        setConnectionParametersForRequest(okHttpRequestBuilder, request);


        for (Interceptor interceptor : mClientBuilder.interceptors()) {
            mClientBuilder.addNetworkInterceptor(interceptor);
        }

        OkHttpClient client = mClientBuilder.build();
        okhttp3.Request okHttpRequest = okHttpRequestBuilder.build();
        Call okHttpCall = client.newCall(okHttpRequest);
        okhttp3.Response okHttpResponse = okHttpCall.execute();


        int code = okHttpResponse.code();
        ResponseBody body = okHttpResponse.body();
        InputStream content = body == null ? null : body.byteStream();
        int contentLength = body == null ? 0 : (int) body.contentLength();
        List<Header> responseHeaders = mapHeaders(okHttpResponse.headers());
        return new HttpResponse(code, responseHeaders, contentLength, content);
    }

    private List<Header> mapHeaders(Headers responseHeaders) {
        List<Header> headers = new ArrayList<>();
        for (int i = 0, len = responseHeaders.size(); i < len; i++) {
            final String name = responseHeaders.name(i), value = responseHeaders.value(i);
            if (name != null) {
                headers.add(new Header(name, value));
            }
        }
        return headers;
    }
}
