package org.wordpress.android.fluxc.tools;

import android.graphics.Bitmap;
import android.util.Base64;
import android.widget.ImageView.ScaleType;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;

import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.HTTPAuthModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.utils.WPUrlUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Image Loader that leverage the Volley queue, stored access token and stored HTTP Auth credentials
 */
public class FluxCImageLoader extends ImageLoader {
    private AccessToken mAccessToken;
    private HTTPAuthManager mHTTPAuthManager;
    private UserAgent mUserAgent;

    @Inject public FluxCImageLoader(@Named("custom-ssl") RequestQueue queue,
                            ImageCache imageCache,
                            AccessToken accessToken,
                            HTTPAuthManager httpAuthManager,
                            UserAgent userAgent) {
        super(queue, imageCache);
        mAccessToken = accessToken;
        mHTTPAuthManager = httpAuthManager;
        mUserAgent = userAgent;
        // http://stackoverflow.com/a/17035814 - Responses from the ImageLoader are actually delayed / batched
        // up before being delivered. So images that are ready are not being delivered as soon as they
        // possible can be to achieve a sort of page load aesthetic.
        setBatchedResponseDelay(0);
    }

    @Override
    protected Request<Bitmap> makeImageRequest(String requestUrl, int maxWidth, int maxHeight,
                                               ScaleType scaleType, final String cacheKey) {
        if (WPUrlUtils.isWordPressCom(requestUrl) && !UrlUtils.isHttps(requestUrl)) {
            requestUrl = UrlUtils.makeHttps(requestUrl);
        }
        final String url = requestUrl;
        return new ImageRequest(url, new Response.Listener<Bitmap>() {
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
                HashMap<String, String> headers = new HashMap<>();
                headers.put("User-Agent", mUserAgent.getUserAgent());
                if (WPUrlUtils.safeToAddWordPressComAuthToken(url)) {
                    headers.put("Authorization", "Bearer " + mAccessToken.get());
                } else {
                    // Check if we had HTTP Auth credentials for the root url
                    HTTPAuthModel httpAuthModel = mHTTPAuthManager.getHTTPAuthModel(url);
                    if (httpAuthModel != null) {
                        String creds = String.format("%s:%s", httpAuthModel.getUsername(), httpAuthModel.getPassword());
                        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                        headers.put("Authorization", auth);
                    }
                }
                return headers;
            }
        };
    }
}

