package org.wordpress.android.mocks;

import org.wordpress.android.networking.AuthenticatorRequest;
import org.wordpress.android.networking.OAuthAuthenticator;

public class OAuthAuthenticatorEmptyMock extends OAuthAuthenticator {
    public void authenticate(AuthenticatorRequest request) {
    }
}
