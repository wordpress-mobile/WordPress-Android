package org.wordpress.android.fluxc.utils;

import com.android.volley.Request.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.rest.GsonRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class GsonRequestTest {
    @Test
    public void testAddParamsToUrlIfGet() {
        String baseUrl = "https://public-api.wordpress.com/rest/v1.1/sites/56/posts/";
        Map<String, String> params = new HashMap<>();

        params.put("type", "post");
        assertEquals(baseUrl + "?type=post", GsonRequest.addParamsToUrlIfGet(Method.GET, baseUrl, params));

        params.put("offset", "20");
        assertEquals(baseUrl + "?offset=20&type=post", GsonRequest.addParamsToUrlIfGet(Method.GET, baseUrl, params));

        // No change to URL if params are null or empty
        assertEquals(baseUrl, GsonRequest.addParamsToUrlIfGet(Method.GET, baseUrl, null));
        assertEquals(baseUrl, GsonRequest.addParamsToUrlIfGet(Method.GET, baseUrl,
                Collections.<String, String>emptyMap()));

        // No change to URL if method is not GET
        assertEquals(baseUrl, GsonRequest.addParamsToUrlIfGet(Method.POST, baseUrl, null));
        assertEquals(baseUrl, GsonRequest.addParamsToUrlIfGet(Method.POST, baseUrl,
                Collections.<String, String>emptyMap()));
        assertEquals(baseUrl, GsonRequest.addParamsToUrlIfGet(Method.POST, baseUrl, params));
    }
}
