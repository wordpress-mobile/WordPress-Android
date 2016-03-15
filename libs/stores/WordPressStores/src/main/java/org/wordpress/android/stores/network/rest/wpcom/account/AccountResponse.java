package org.wordpress.android.stores.network.rest.wpcom.account;

import org.wordpress.android.stores.network.Response;

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
