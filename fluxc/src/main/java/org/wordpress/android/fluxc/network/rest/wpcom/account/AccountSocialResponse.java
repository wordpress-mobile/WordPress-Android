package org.wordpress.android.fluxc.network.rest.wpcom.account;

import org.json.JSONArray;
import org.wordpress.android.fluxc.network.Response;

public class AccountSocialResponse implements Response {
    public JSONArray two_step_supported_auth_types;
    public String bearer_token;
    public String phone_number;
    public String two_step_nonce;
    public String two_step_nonce_authenticator;
    public String two_step_nonce_backup;
    public String two_step_nonce_sms;
    public String two_step_nonce_webauthn;
    public String two_step_notification_sent;
    public String user_id;
    public String username;
    public boolean created_account;
}
