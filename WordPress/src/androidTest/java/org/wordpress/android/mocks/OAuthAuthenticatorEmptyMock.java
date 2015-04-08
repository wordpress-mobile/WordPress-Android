package org.wordpress.android.mocks;

import org.wordpress.android.networking.AuthenticatorRequest;
import org.wordpress.android.networking.OAuthAuthenticator;
import org.wordpress.android.util.AccountHelper;

public class OAuthAuthenticatorEmptyMock extends OAuthAuthenticator {
    public void authenticate(AuthenticatorRequest request) {
        AccountHelper.getDefaultAccount().setAccessToken("dead-parrot");
    }
}
