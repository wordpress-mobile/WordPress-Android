package org.wordpress.android.fluxc.network.rest.wpcom.account;

import org.wordpress.android.fluxc.network.Response;

/**
 * Stores data retrieved from the WordPress.com REST API Account endpoint (/me). Field names
 * correspond to REST response keys.
 *
 * See <a href="https://developer.wordpress.com/docs/api/1.1/get/me/">documentation</a>
 */
public class AccountResponse implements Response {
    public long ID;
    public String display_name;
    public String username;
    public String email;
    public long primary_blog;
    public String avatar_URL;
    public String profile_URL;
    public boolean email_verified;
    public String date;
    public int site_count;
    public int visible_site_count;
    public boolean has_unseen_notes;
    public String user_ip_country_code;
}
