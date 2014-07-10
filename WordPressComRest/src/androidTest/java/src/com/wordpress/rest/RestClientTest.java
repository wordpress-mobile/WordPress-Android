package com.wordpress.rest;

import android.test.AndroidTestCase;

public class RestClientTest extends AndroidTestCase {
    
    public void testGetAbsoluteURLWithPath(){
        String path = "me";
        String url = RestClient.getAbsoluteURL(path);
        String expected = String.format("%s%s", RestClient.REST_API_ENDPOINT_URL, path);
        assertEquals(expected, url);
    }
    
    public void testGetAbsoluteURLWithLeadingSlash(){
        String path = "/sites/mobileprojects.wordpress.com/posts";
        String url = RestClient.getAbsoluteURL(path);
        String expected = String.format("https://public-api.wordpress.com/rest/v1%s", path);
        assertEquals(expected, url);
    }
    
    public void testGetAbsoluteURLWithFullURL(){
        String expected = String.format("%s%s", RestClient.REST_API_ENDPOINT_URL, "notes");
        assertEquals(expected, RestClient.getAbsoluteURL(expected));
    }
    
}