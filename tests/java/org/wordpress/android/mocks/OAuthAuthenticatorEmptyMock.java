package org.wordpress.android.mocks;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.wordpress.android.WordPress;
import org.wordpress.android.networking.AuthenticatorRequest;
import org.wordpress.android.networking.OAuthAuthenticator;

public class OAuthAuthenticatorEmptyMock extends OAuthAuthenticator {
    public void authenticate(AuthenticatorRequest request) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        settings.edit().putString(WordPress.ACCESS_TOKEN_PREFERENCE, "dead-parrot").commit();
    }
}
