package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.network.Response;

public class DomainSuggestionResponse implements Response {
    public String cost;
    public String domain_name;
    public boolean is_free;
    public boolean is_premium;
    public boolean supports_privacy;

    public int product_id;
    public String product_slug;
    public String vendor;
    public float relevance;
}
