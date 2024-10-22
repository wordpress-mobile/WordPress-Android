package org.wordpress.android.fluxc.network.rest.wpcom.account;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.Map;

public class AccountSocialRequest extends BaseRequest<AccountSocialResponse> {
    private static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT = "application/x-www-form-urlencoded";

    private final Map<String, String> mParams;
    private final Response.Listener<AccountSocialResponse> mListener;

    public AccountSocialRequest(String url, Map<String, String> params,
                                Response.Listener<AccountSocialResponse> listener, BaseErrorListener errorListener) {
        super(Method.POST, url, errorListener);
        mParams = params;
        mListener = listener;
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        return error;
    }

    @Override
    protected void deliverResponse(AccountSocialResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public String getBodyContentType() {
        return String.format("%s; charset=%s", PROTOCOL_CONTENT, PROTOCOL_CHARSET);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }

    @Override
    protected Response<AccountSocialResponse> parseNetworkResponse(NetworkResponse response) {
        try {
            AccountSocialResponse parsed = new AccountSocialResponse();
            String responseBody = new String(response.data);
            JSONObject object = new JSONObject(responseBody);
            JSONObject data = object.getJSONObject("data");
            parsed.bearer_token = data.optString("bearer_token");
            parsed.phone_number = data.optString("phone_number");
            parsed.two_step_nonce = data.optString("two_step_nonce");
            parsed.two_step_supported_auth_types = data.optJSONArray("two_step_supported_auth_types");
            parsed.two_step_nonce_authenticator = data.optString("two_step_nonce_authenticator");
            parsed.two_step_nonce_backup = data.optString("two_step_nonce_backup");
            parsed.two_step_nonce_sms = data.optString("two_step_nonce_sms");
            parsed.two_step_nonce_webauthn = data.optString("two_step_nonce_webauthn");
            parsed.two_step_notification_sent = data.optString("two_step_notification_sent");
            parsed.user_id = data.optString("user_id");
            parsed.username = data.optString("username");
            parsed.created_account = data.optBoolean("created_account");
            return Response.success(parsed, null);
        } catch (JSONException exception) {
            AppLog.e(T.API, "Unable to parse network response: " + exception.getMessage());
            return null;
        }
    }
}
