package org.wordpress.android.stores.network.rest.wpcom.site;

import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.network.Response;

import java.util.List;

public class SiteWPComRestResponse implements Payload, Response {
    public class SitesResponse {
        public List<SiteWPComRestResponse> sites;
    }

    public class Options {
        public boolean videopress_enabled;
        public boolean featured_images_enabled;
        public String admin_url;
    }

    public int ID;
    public String URL;
    public String name;
    public String description;
    public boolean jetpack;
    public boolean visible;
    public Options options;
}


