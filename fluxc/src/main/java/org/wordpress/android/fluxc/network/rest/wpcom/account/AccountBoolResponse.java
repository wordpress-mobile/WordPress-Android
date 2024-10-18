package org.wordpress.android.fluxc.network.rest.wpcom.account;

import org.wordpress.android.fluxc.network.Response;

public class AccountBoolResponse implements Response {
    public boolean success;
    public String error;
    public String message;
}
