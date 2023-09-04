package org.wordpress.android.fluxc.network.rest.wpcom.taxonomy;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

public class TermWPComRestResponse implements Response {
    public static class TermsResponse {
        public List<TermWPComRestResponse> terms;
    }

    public long ID;
    public String name;
    public String slug;
    public String description;
    public int post_count;
    public long parent;
    public Meta meta;

    public static class Meta {
        public Links links;

        public static class Links {
            public String self;
            public String help;
            public String site;
        }
    }
}
