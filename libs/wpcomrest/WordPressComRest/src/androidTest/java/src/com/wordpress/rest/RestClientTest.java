package com.wordpress.rest;

import android.test.AndroidTestCase;

public class RestClientTest extends AndroidTestCase {
    public void testGetAbsoluteURLWithLeadingSlash(){
        String path = "/sites/mobileprojects.wordpress.com/posts";
        RestClient restClient = new RestClient(null);
        String url = restClient.getAbsoluteURL(path);
        String expected = String.format("https://public-api.wordpress.com/rest/v1%s", path);
        assertEquals(expected, url);
    }
}
