package org.wordpress.android.networking;

import android.graphics.Bitmap;
import android.widget.ImageView.ScaleType;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;

import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.util.WPUrlUtils;

import java.util.HashMap;
import java.util.Map;

public class WPImageLoader extends ImageLoader {
    private AccessToken mAccessToken;

    public WPImageLoader(RequestQueue queue, ImageCache imageCache, AccessToken accessToken) {
        super(queue, imageCache);
        mAccessToken = accessToken;
    }

    @Override
    protected Request<Bitmap> makeImageRequest(final String requestUrl, int maxWidth, int maxHeight,
                                               ScaleType scaleType, final String cacheKey) {
        return new ImageRequest(requestUrl, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                onGetImageSuccess(cacheKey, response);
            }
        }, maxWidth, maxHeight, scaleType, Bitmap.Config.RGB_565, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onGetImageError(cacheKey, error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                if (WPUrlUtils.safeToAddWordPressComAuthToken(requestUrl)) {
                    headers.put("Authorization", "Bearer " + mAccessToken.get());
                }
                return headers;
            }
        };
    }
}
