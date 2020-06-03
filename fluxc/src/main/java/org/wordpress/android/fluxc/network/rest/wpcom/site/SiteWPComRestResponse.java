package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

public class SiteWPComRestResponse implements Response {
    public class SitesResponse {
        public List<SiteWPComRestResponse> sites;
    }

    public class Options {
        public boolean videopress_enabled;
        public boolean featured_images_enabled;
        public boolean is_automated_transfer;
        public boolean is_wpcom_atomic;
        public boolean is_wpcom_store;
        public boolean woocommerce_is_active;
        public String admin_url;
        public String login_url;
        public String gmt_offset;
        public String frame_nonce;
        public String unmapped_url;
        public String max_upload_size;
        public String wp_max_memory_limit;
        public String wp_memory_limit;
        public String jetpack_version;
        public String software_version;
        public String show_on_front;
        public long page_on_front;
        public long page_for_posts;
    }

    public class Plan {
        public String product_id;
        public String product_name_short;
        public boolean is_free;
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

    public class Quota {
        public long space_allowed;
        public long space_used;
        public double percent_used;
        public long space_available;
    }

    public class Icon {
        public String img;
    }


    public class Meta {
        public class Links {
            public String xmlrpc;
        }

        public Links links;
    }

    public long ID;
    public String URL;
    public String name;
    public String description;
    public boolean jetpack;
    public boolean visible;
    public boolean is_private;
    public boolean is_coming_soon;
    public Options options;
    public Capabilities capabilities;
    public Plan plan;
    public Icon icon;
    public Meta meta;
    public Quota quota;
}
