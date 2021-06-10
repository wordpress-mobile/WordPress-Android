package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.network.Response;

public class NewSiteResponse implements Response {
    public boolean success;
    public BlogDetails blog_details;
    public String error;
    public String message;

    public static class BlogDetails {
        public String url;
        public String blogid;
    }
}
