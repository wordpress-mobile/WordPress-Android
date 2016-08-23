package org.wordpress.android.fluxc.network.rest;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.wordpress.android.fluxc.network.BaseRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public abstract class GsonRequest<T> extends BaseRequest<T> {
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    private final Gson mGson = new Gson();
    private final Class<T> mClass;
    private final Listener<T> mListener;
    private final Map<String, String> mParams;

    public GsonRequest(int method, Map<String, String> params, String url, Class<T> clazz, Listener<T> listener,
                       BaseErrorListener errorListener) {
        super(method, addParamsToUrlIfGet(method, url, params), errorListener);
        mClass = clazz;
        mListener = listener;
        mParams = params;
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    protected Map<String, String> getParams() {
        return mParams;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(mGson.fromJson(json, mClass), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

    public static String addParamsToUrlIfGet(int method, String url, Map<String, String> params) {
        if (method != Request.Method.GET || params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()){
            if (stringBuilder.length() == 0){
                stringBuilder.append('?');
            } else {
                stringBuilder.append('&');
            }
            stringBuilder.append(entry.getKey()).append('=').append(entry.getValue());
        }

        return url + stringBuilder.toString();
    }
}
