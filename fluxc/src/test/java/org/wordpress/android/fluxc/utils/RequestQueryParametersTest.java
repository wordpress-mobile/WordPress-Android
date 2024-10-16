package org.wordpress.android.fluxc.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class RequestQueryParametersTest {
    @Test
    public void testBaseRequestAddQueryParameters() {
        String baseUrl = "https://public-api.wordpress.com/rest/v1.1/sites/56/posts/";

        WPComGsonRequest<Object> wpComGsonRequest = WPComGsonRequest.buildGetRequest(baseUrl, null, null, null, null);

        wpComGsonRequest.addQueryParameter("type", "post");
        assertEquals(baseUrl + "?type=post", wpComGsonRequest.getUrl());

        Map<String, String> params = new HashMap<>();
        params.put("offset", "20");
        params.put("favorite_pet", "pony");
        wpComGsonRequest.addQueryParameters(params);
        assertEquals(baseUrl + "?type=post&offset=20&favorite_pet=pony", wpComGsonRequest.getUrl());

        // No change to URL if params are null or empty
        wpComGsonRequest.addQueryParameters(null);
        assertEquals(baseUrl + "?type=post&offset=20&favorite_pet=pony", wpComGsonRequest.getUrl());

        wpComGsonRequest.addQueryParameters(new HashMap<String, String>());
        assertEquals(baseUrl + "?type=post&offset=20&favorite_pet=pony", wpComGsonRequest.getUrl());
    }

    @Test
    public void testWPComGsonRequestConstructorGet() {
        String baseUrl = "https://public-api.wordpress.com/rest/v1.1/sites/56/posts/";

        Map<String, String> params = new HashMap<>();
        params.put("offset", "20");
        params.put("favorite_pet", "pony");

        WPComGsonRequest wpComGsonRequest = WPComGsonRequest.buildGetRequest(baseUrl, params, null, null, null);
        assertEquals(baseUrl + "?offset=20&favorite_pet=pony", wpComGsonRequest.getUrl());
    }

    @Test
    public void testWPComGsonRequestConstructorPost() {
        String baseUrl = "https://public-api.wordpress.com/rest/v1.1/sites/56/posts/";

        Map<String, Object> body = new HashMap<>();
        body.put("offset", "20");
        body.put("favorite_pet", "pony");

        WPComGsonRequest wpComGsonRequest = WPComGsonRequest.buildPostRequest(baseUrl, body, null, null, null);
        // No change if the request != GET
        assertEquals(baseUrl, wpComGsonRequest.getUrl());
    }
}
