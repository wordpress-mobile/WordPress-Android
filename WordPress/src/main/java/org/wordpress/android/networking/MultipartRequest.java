package org.wordpress.android.networking;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;

/**
 * Created by hypest on 24/03/16.
 */
public class MultipartRequest extends RestRequest {
    private final String mMimeType;
    private final byte[] mMultipartBody;

    public MultipartRequest(String url, String mimeType, byte[] multipartBody, Response.Listener<JSONObject>
            listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, null, listener, errorListener);
        this.mMimeType = mimeType;
        this.mMultipartBody = multipartBody;
    }

    @Override
    public String getBodyContentType() {
        return mMimeType;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return mMultipartBody;
    }
 }