package org.wordpress.android.fluxc.network.rest.wpcom.account;

import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse;

import java.util.List;

public class IsAvailableResponse extends JsonObjectOrFalse {
    public String error;
    public String message;
    public String status;
    public List<String> suggestions; // /is-available/domain only
}
