package org.wordpress.android.util;

import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OEmbedUtils {
    private static final Map<Pattern, String> PROVIDERS;

    static {
        PROVIDERS = new HashMap<Pattern, String>();
        PROVIDERS.put(Pattern.compile("(http;//.*youtube\\.com/\\S+)"), "http://www.youtube.com/oembed?");
        PROVIDERS.put(Pattern.compile("(https;//.*youtube\\.com/\\S+)"), "http://www.youtube.com/oembed?scheme=https&");
        PROVIDERS.put(Pattern.compile("(http://youtu\\.be/\\S+)"), "http://www.youtube.com/oembed?");
        PROVIDERS.put(Pattern.compile("(https://youtu\\.be/\\S+)"), "http://www.youtube.com/oembed?scheme=https&");
    }

    public interface Callback {
        public void onSuccess(String inputUrl, String output);
        public void onFailure(Throwable t);
    }

    private static void autoDetectReplaceableOEmbeds(String text, final Callback callback) {
        for (Pattern providerPattern : PROVIDERS.keySet()) {
            Matcher matcher = providerPattern.matcher(text);
            while (matcher.find()) {
                String providerUrl = matcher.group(1);
                requestOEmbedUrl(providerUrl, PROVIDERS.get(providerPattern), callback);
            }
        }
    }

    private static void requestOEmbedUrl(final String sourceUrl, final String oembedRequestUrl,
                                         final Callback callback) {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("url", sourceUrl));
        String fullUrl = oembedRequestUrl +  URLEncodedUtils.format(parameters, HTTP.UTF_8);
        StringRequest req = new StringRequest(Request.Method.GET, fullUrl, new Listener<String>() {
            @Override
            public void onResponse(String response) {
                String output = null;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    output = jsonObject.getString("html");
                } catch (JSONException e) {
                    callback.onFailure(e);
                }
                if (output != null) {
                    callback.onSuccess(sourceUrl, output);
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onFailure(error);
            }
        });
        WordPress.requestQueue.add(req);
    }

    public static void autoEmbedUrl(String text, final Callback callback) {
        autoDetectReplaceableOEmbeds(text, callback);
    }
}
