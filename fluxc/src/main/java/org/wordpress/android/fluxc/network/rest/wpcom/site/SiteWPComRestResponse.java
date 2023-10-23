package org.wordpress.android.fluxc.network.rest.wpcom.site;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;
import java.util.Map;

public class SiteWPComRestResponse implements Response {
    public static class SitesResponse {
        public List<SiteWPComRestResponse> sites;
    }

    public static class Options {
        public boolean videopress_enabled;
        public boolean featured_images_enabled;
        public boolean is_automated_transfer;
        public boolean is_wpcom_atomic;
        public boolean is_wpcom_store;
        public boolean woocommerce_is_active;
        public boolean is_wpforteams_site;
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
        public boolean publicize_permanently_disabled;
        public List<String> active_modules;
        public List<String> jetpack_connection_active_plugins;
        public BloggingPromptsSettings blogging_prompts_settings;
        public int blog_public;
        public boolean can_blaze;
    }

    public static class Plan {
        public String product_id;
        public String product_name_short;
        public String product_slug;
        public boolean is_free;
        @Nullable public Features features;
    }

    public class Features {
        @Nullable public List<String> active;
    }

    public static class Capabilities {
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

    public static class Quota {
        public long space_allowed;
        public long space_used;
        public double percent_used;
        public long space_available;
    }

    public static class Icon {
        public String img;
    }


    public static class Meta {
        public static class Links {
            public String xmlrpc;
        }

        public Links links;
    }

    public class ZendeskSiteMeta {
        public String plan;
        public List<String> addon;
    }

    public class BloggingPromptsSettings {
        public boolean prompts_card_opted_in;
        public boolean prompts_reminders_opted_in;
        public boolean is_potential_blogging_site;
        public Map<String, Boolean> reminders_days;
        public String reminders_time;
    }

    public long ID;
    public String URL;
    public String name;
    public String description;
    public boolean jetpack;
    public boolean jetpack_connection;
    public boolean visible;
    public boolean is_private;
    public boolean is_coming_soon;
    public int organization_id;
    public Options options;
    public Capabilities capabilities;
    public Plan plan;
    public Icon icon;
    public Meta meta;
    public Quota quota;
    public ZendeskSiteMeta zendesk_site_meta;
    public boolean was_ecommerce_trial;
    public boolean single_user_site;
}
