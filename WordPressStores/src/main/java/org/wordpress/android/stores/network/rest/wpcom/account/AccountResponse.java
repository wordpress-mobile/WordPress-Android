package org.wordpress.android.stores.network.rest.wpcom.account;

import org.wordpress.android.stores.network.Response;

/**
 * Stores data retrieved from the WordPress.com REST API Account endpoint (/me). Field names
 * correspond to REST response keys.
 *
 * See <a href="https://developer.wordpress.com/docs/api/1.1/get/me/">documentation</a>
 */
public class AccountResponse implements Response {
    public String username;
    public long ID;
    public String display_name;
    public String profile_URL;
    public String avatar_URL;
    public long primary_blog;
    public int site_count;
    public int visible_site_count;
    public String email;
}
