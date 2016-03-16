package org.wordpress.android.stores.network.rest.wpcom.account;

import org.wordpress.android.stores.network.Response;

/**
 * Stores data retrieved from the WordPress.com REST API Account Settings endpoint (/me/settings).
 * Field names correspond to REST response keys.
 *
 * See <a href="https://developer.wordpress.com/docs/api/1.1/get/me/settings">documentation</a>
 */
public class AccountSettingsResponse implements Response {
    public String user_login;
    public long primary_site_ID;
    public String first_name;
    public String last_name;
    public String description;
    public String date;
    public String new_user_email;
    public boolean user_email_change_pending;
    public String user_URL;
}
