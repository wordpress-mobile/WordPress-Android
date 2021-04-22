package org.wordpress.android.fluxc.network.rest.wpcom.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.fluxc.utils.PreferenceUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccessToken {
    private static final String ACCOUNT_TOKEN_PREF_KEY = "ACCOUNT_TOKEN_PREF_KEY";
    private String mToken;
    private Context mContext;

    @Inject public AccessToken(Context appContext) {
        mContext = appContext;
        mToken = getFluxCPreferences().getString(ACCOUNT_TOKEN_PREF_KEY, "");
        if (mToken.isEmpty()) {
            // Check the old token storage location, since we might init the access token
            // before the token migration completes (DB upgrade 38 -> 39)
            mToken = PreferenceManager.getDefaultSharedPreferences(mContext).getString(ACCOUNT_TOKEN_PREF_KEY, "");
        }
    }

    public boolean exists() {
        return !TextUtils.isEmpty(mToken);
    }

    public String get() {
        return mToken;
    }

    public void set(String token) {
        mToken = token;
        getFluxCPreferences().edit().putString(ACCOUNT_TOKEN_PREF_KEY, token).apply();
    }

    private SharedPreferences getFluxCPreferences() {
        return PreferenceUtils.getFluxCPreferences(mContext);
    }
}
