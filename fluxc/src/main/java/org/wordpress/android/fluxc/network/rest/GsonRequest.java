package org.wordpress.android.fluxc.network.rest;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.Nullable;
import org.wordpress.android.fluxc.network.BaseRequest;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class GsonRequest<T> extends BaseRequest<T> {
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    private final Gson mGson;
    private final Class<T> mClass;
    private final Type mType;
    private final Listener<T> mListener;
    private final Map<String, String> mParams;
    private final Map<String, Object> mBody;

    protected GsonRequest(int method, Map<String, String> params, Map<String, Object> body, String url, Class<T> clazz,
                       Type type, Listener<T> listener, BaseErrorListener errorListener) {
        super(method, url, errorListener);
        // HTTP RFC requires a body (even empty) for all POST requests. Volley will default to using the params
        // for the body so only do this if params is null since this behavior is desirable for form-encoded
        // POST requests.
        if (method == Method.POST && body == null && (params == null || params.size() == 0)) {
            body = new HashMap<>();
        }

        mClass = clazz;
        mType = type;
        mListener = listener;
        mGson = setupGsonBuilder().create();
        mParams = params;
        mBody = body;
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    public String getBodyContentType() {
        if (mBody == null) {
            return super.getBodyContentType();
        } else {
            return PROTOCOL_CONTENT_TYPE;
        }
    }

    @Override
    protected Map<String, String> getParams() {
        return mParams;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (mBody == null) {
            return super.getBody();
        }

        return mGson.toJson(mBody).getBytes(Charset.forName("UTF-8"));
    }

    @Nullable
    protected Map<String, Object> getBodyAsMap() {
        return mBody;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            Log.d("vojta", json);
            T res;
            if (mClass == null) {
                res = mGson.fromJson(json, mType);
            } else {
                res = mGson.fromJson(json, mClass);
            }
            return Response.success(res, createCacheEntry(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }

    private GsonBuilder setupGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setLenient();
        gsonBuilder.registerTypeHierarchyAdapter(JsonObjectOrFalse.class, new JsonObjectOrFalseDeserializer());
        gsonBuilder.registerTypeHierarchyAdapter(JsonObjectOrEmptyArray.class,
                new JsonObjectOrEmptyArrayDeserializer());
        return gsonBuilder;
    }
}
