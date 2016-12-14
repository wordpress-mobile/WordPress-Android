package org.wordpress.android.fluxc.network.rest.wpcom.taxonomy;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.Response;

import java.util.List;

public class TermWPComRestResponse extends Payload implements Response {
    public class TermsResponse {
        public List<TermWPComRestResponse> terms;
    }

    public long ID;
    public String name;
    public String slug;
    public String description;
    public long post_count;
    public long parent;
    public Meta meta;

    public class Meta {
        public Links links;

        public class Links {
            public String self;
            public String help;
            public String site;
        }
    }
}
