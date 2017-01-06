package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.network.Response;

import java.util.List;

public class SiteWPComRestResponse extends Payload implements Response {
    public class SitesResponse {
        public List<SiteWPComRestResponse> sites;
    }

    public class Options {
        public boolean videopress_enabled;
        public boolean featured_images_enabled;
        public String admin_url;
        public String login_url;
        public String timezone;
    }

    public class Plan {
        public long product_id;
        public String product_name_short;
    }

    public class Capabilities {
        public boolean edit_pages;
        public boolean edit_posts;
        public boolean edit_others_posts;
        public boolean edit_others_pages;
        public boolean delete_posts;
        public boolean delete_others_posts;
        public boolean edit_theme_options;
        public boolean edit_users;
        public boolean list_users;
        public boolean manage_categories;
        public boolean manage_options;
        public boolean activate_wordads;
        public boolean promote_users;
        public boolean publish_posts;
        public boolean upload_files;
        public boolean delete_user;
        public boolean remove_users;
        public boolean view_stats;
    }


    public class Meta {
        public class Links {
            public String xmlrpc;
        }

        public Links links;
    }

    public int ID;
    public String URL;
    public String name;
    public String description;
    public boolean jetpack;
    public boolean visible;
    public boolean is_private;
    public Options options;
    public Capabilities capabilities;
    public Plan plan;
    public Meta meta;
}
